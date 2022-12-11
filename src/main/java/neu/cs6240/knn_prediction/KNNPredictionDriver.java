package neu.cs6240.knn_prediction;

import javafx.util.Pair;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.*;

public class KNNPredictionDriver extends Configured implements Tool {
    private static final Logger logger = LogManager.getLogger(KNNPredictionDriver.class);

    public static enum AccuracyCounters {
        TEST_RECORDS,
        CORRECT_PREDICTION
    }

    public static class KNNPredictionMapper extends Mapper<LongWritable, Text, Text, Text> {
        private HashMap<String, PriorityQueue<Pair<Double, String>>> map = new HashMap<>();
        private final static int K = 10;

        @Override
        public void map(final LongWritable offset, final Text lineText, final Context context) throws IOException, InterruptedException {
            //split incomnig record for test : train
            String[] records = lineText.toString().split(":");
            String testRecord = records[0];
            String trainRecord = records[1];
            String trainGenre = trainRecord.split(",")[1];

            //get distance between test and train records
            double dist = calcDistance(trainRecord, testRecord);
            String[] testSplit = testRecord.split(",");

            //update map with K closest train records to each test record
            String testTrackID = testSplit[0];
            String testGenre = testSplit[1];
            String key = testTrackID + "," + testGenre;

            //TODO: add Priority queue if not exist
            if(map.get(key) == null) {
                map.put(key, new PriorityQueue<>((p1, p2) -> Double.compare(p2.getKey(), p1.getKey())));
            }
            map.get(key).add(new Pair(dist, trainGenre));
            if(map.get(key).size() > K) {
                map.get(key).poll();
            }
        }

        public double calcDistance(String trainRecord, String testRecord) {
            List<String> train = Arrays.asList(trainRecord.split(","));
            List<String> test = Arrays.asList(testRecord.split(","));
            if(train.size() != test.size()) return Double.MAX_VALUE;
            double distance = 0.0;

            //train and size should have the same number of lyric attributes
            for(int i = 2; i < train.size(); i++) {
                Double trainVal = Double.parseDouble(train.get(i));
                Double testVal = Double.parseDouble(test.get(i));
                distance += (double)Math.pow(trainVal - testVal, 2);
            }
            return Math.sqrt(distance);
        }

        @Override
        public void cleanup(Context context) throws IOException, InterruptedException {
            for(String key : map.keySet()) {
                Text newKey = new Text(key);
                Text newValue = new Text();
                while(!map.get(key).isEmpty()) {
                    Pair p = map.get(key).poll();
                    newValue.set(p.getKey() + "," + p.getValue());
                    context.write(newKey, newValue);
                }
            }
        }
    }

    public static class KNNPredictionReducer extends Reducer<Text, Text, Text, Text> {
        private static final int K = 10;

        @Override
        public void reduce(final Text key, final Iterable<Text> values, final Context context) throws IOException, InterruptedException {
            //Key should be each Test Record's TrackID + Genre
            String[] keySplit = key.toString().split(",");
            String trackID = keySplit[0];
            String testGenre = keySplit[1];

            //create MaxHeap to keep track of the K closest Genre's to this Test Record key
            PriorityQueue<Pair<Double, String>> pq = new PriorityQueue<>((p1, p2) -> Double.compare(p2.getKey(), p1.getKey()));
            double minDistance = Double.MAX_VALUE;
            for (Text t : values) {
                String[] parts = t.toString().split(",");
                Double dist = Double.parseDouble(parts[0]);
                String genre = parts[1];
                minDistance = Math.min(dist, minDistance);
                pq.add(new Pair(dist, genre));

                if(pq.size() > K) {
                    pq.poll();
                }
            }

            HashMap<String, Double> genreMap = new HashMap<>();
            double maxDistance = pq.peek().getKey(), range = maxDistance - minDistance, maxGenreVal = 0.0;
            String predictGenre = null;
            while(!pq.isEmpty()) {
                Pair<Double, String> p = pq.poll();
                genreMap.put(p.getValue(), genreMap.getOrDefault(p.getValue(), 0.0) + (range / p.getKey()));
                if(genreMap.get(p.getValue()) > maxGenreVal) {
                    maxGenreVal = genreMap.get(p.getValue());
                    predictGenre = p.getValue();
                }
            }

            //update counters
            context.getCounter(KNNPredictionDriver.AccuracyCounters.TEST_RECORDS).increment(1);
            if(predictGenre.equals(testGenre)) {
                context.getCounter(KNNPredictionDriver.AccuracyCounters.CORRECT_PREDICTION).increment(1);
            }

            //write to context
            context.write(new Text(trackID), new Text(predictGenre));
        }
    }

    public int run(final String[] args) throws Exception {
        final Configuration conf = getConf();
        String[] otherArgs = new GenericOptionsParser(conf, args)
                .getRemainingArgs();
        final Job job = Job.getInstance(conf, "CS6240 Final Project Data Processor");
        job.setJarByClass(KNNPredictionDriver.class);
        final Configuration jobConf = job.getConfiguration();

        job.setMapperClass(KNNPredictionMapper.class);
        job.setReducerClass(KNNPredictionReducer.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        int result = job.waitForCompletion(true) ? 0 : 1;

        //Counters
        Counters counters = job.getCounters();
        Counter c1 = counters.findCounter(AccuracyCounters.TEST_RECORDS);
        System.out.println(c1.getDisplayName() + ":  " + c1.getValue());
        Counter c2 = counters.findCounter(AccuracyCounters.CORRECT_PREDICTION);
        System.out.println(c2.getDisplayName() + ":  " + c2.getValue());
        System.out.println("KNN Prediction Accuracy: " + (((double)c2.getValue()) / ((double)c1.getValue())));

        return result;
    }

    public static void main(final String[] args) {
        if (args.length != 2) {
            throw new Error("Two arguments required:\n<input-dir> <output-dir>");
        }
        try {
            ToolRunner.run(new KNNPredictionDriver(), args);
        } catch (final Exception e) {
            logger.error("", e);
        }
    }
}
