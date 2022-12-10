package neu.cs6240.knn_prediction;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
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

import java.io.*;
import java.net.URI;
import java.util.*;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

public class KNNPredictionDriver extends Configured implements Tool {
    private static final Logger logger = LogManager.getLogger(KNNPredictionDriver.class);

    public static enum AccuracyCounters {
        TEST_RECORDS,
        CORRECT_PREDICTION
    }

    public static class KNNPredictionMapper extends Mapper<LongWritable, Text, Text, Text> {
        private ArrayList<String> testFile;
        private HashMap<String, PriorityQueue<Pair<Double, String>>> map;
        private final static int K = 10;

        @Override
        public void setup(Context context) throws IOException, InterruptedException {
            testFile = new ArrayList<>();
            map = new HashMap<>();
            try {
                URI[] files = context.getCacheFiles();

                if (files == null || files.length == 0) {
                    throw new RuntimeException("Test file is not set in DistributedCache");
                }

                AWSCredentials credentials = new AWSSessionCredentials() {
                    @Override
                    public String getSessionToken() {
                        return "FwoGZXIvYXdzEFAaDESLGgsbiprlyhNagiK7AZGB5IUvYMNpgDJCBW2wV0S/2YClLeuuMO/D2biqzmxH2mXGxDT4HxUjy1ttpua+nBea8JZMl5xbSDrgO4mjURjXedwt324m++yYwI34z97RjbfJsnuqSOYp15wJCNNJvuSZi1/87xD+RJ2FzTfsN7T4oAXt/O/EQjBKDBVa5rDjWvYz7TRUI8WbgvbHLSNQ5Dq0R7WhXFKe5qlR7Um1FkFndDOcwb5zRjFhEUh6zK2thBleReB6kBf8n58oxe3OnAYyLbksF7gkdxfKdEKEmsjz2quvUbBLFBtAWvOY/C4t6G0qRDaFLdy4mySZrudFgw==";
                    }

                    @Override
                    public String getAWSAccessKeyId() {
                        return "ASIAYZ3LM7D2YEPFX346";
                    }

                    @Override
                    public String getAWSSecretKey() {
                        return "hP+TXyzGi7wGBWtVOF+JUQB8ooiOHtsJdCaqfsvD";
                    }
                };
                AmazonS3 s3client = AmazonS3ClientBuilder
                        .standard()
                        .withCredentials(new AWSStaticCredentialsProvider(credentials))
                        .withRegion(Regions.US_EAST_1)
                        .build();

                for (URI f : files) {
                    S3Object file = s3client.getObject(new GetObjectRequest("junda-cs6240", f.getPath().toString()));
                    InputStream data = file.getObjectContent();
                    InputStreamReader object = new InputStreamReader(data);
                    BufferedReader rdr = new BufferedReader(object);

//                    System.out.println(f.toString());
//                    File file = new File(f);
//                    InputStream r = client.getObject(gor);
//                    InputStreamReader object = new InputStreamReader(r);
//                    BufferedReader rdr = new BufferedReader(object);
                    String line;
                    while ((line = rdr.readLine()) != null) {
                        testFile.add(line);
                        String[] attr = line.split(",");

                        //for each test record, create a max ordered Priority queue of distances. we will only maintain the K smallest distance nodes
                        //attr[0] == unique TrackID from Test File
                        map.put(attr[0] + "," + attr[1], new PriorityQueue<>((p1, p2) -> Double.compare(p2.getKey(), p1.getKey())));
                    }
                    data.close();
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void map(final LongWritable offset, final Text lineText, final Context context) throws IOException, InterruptedException {
            String trainRecord = lineText.toString();
            String trainGenre = trainRecord.split(",")[1];
            for(String testRecord : testFile) {
                String[] testSplit = testRecord.split(",");
                String testTrackID = testSplit[0];
                String testGenre = testSplit[1];
                String key = testTrackID + "," + testGenre;
                double dist = calcDistance(trainRecord, testRecord);

                //add training record's genre and distance from the test record into the test record's Priority Queue
                map.get(key).add(new Pair(dist, trainGenre));

                //keep only the 10 closest training records for this test record
                if(map.get(key).size() > K) {
                    map.get(key).poll();
                }
            }
        }

        public double calcDistance(String trainRecord, String testRecord) {
            //Minkowski Distance calculation
            List<String> train = Arrays.asList(trainRecord.split(","));
            List<String> test = Arrays.asList(testRecord.split(","));
            if(train.size() != test.size()) return Double.MAX_VALUE;
            int q = train.size() - 2;
            double distance = 0.0;

            //train and size should have the same number of lyric attributes
            for(int i = 2; i < train.size(); i++) {
                Double trainVal = Double.parseDouble(train.get(i));
                Double testVal = Double.parseDouble(test.get(i));
                distance += (double)Math.pow(trainVal - testVal, q);
            }
            return Math.pow(distance, (1.0 / (double)q));
        }

        @Override
        public void cleanup(Context context) throws IOException, InterruptedException {
            for(String key : map.keySet()) {
                Text newKey = new Text(key);
                Text newValue = new Text();
                while(!map.get(key).isEmpty()) {
                    Pair p = map.get(key).poll();
                    //key == distance, value == genre
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

    @Override
    public int run(final String[] args) throws Exception {
        final Configuration conf = getConf();
        String[] otherArgs = new GenericOptionsParser(conf, args)
                .getRemainingArgs();
        final Job job = Job.getInstance(conf, "CS6240 Final Project Data Processor");
        job.setJarByClass(KNNPredictionDriver.class);
        final Configuration jobConf = job.getConfiguration();
        jobConf.set("mapreduce.output.textoutputformat.separator", "\t");

        job.setMapperClass(KNNPredictionMapper.class);
        job.setReducerClass(KNNPredictionReducer.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        FileInputFormat.setInputPaths(job, new Path(args[0]));
//        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        FileOutputFormat.setOutputPath(job, new Path(args[2]));

//        int result = job.waitForCompletion(true) ? 0 : 1;

//        S3Client client = S3Client.builder().build();
//        ListObjectsRequest request = ListObjectsRequest.builder().bucket("junda-cs6240").build();
//        client.getResourceURL
//
//        ListObjectsResponse response = client.listObjects(request);
//        List<S3Object> objects = response.contents();
//
//        ListIterator<S3Object> listIterator = objects.listIterator();
//
//        while (listIterator.hasNext()) {
//            S3Object object = listIterator.next();
//            object.
//            System.out.println(object.key() + " - " + object.size());
//        }
//        AWSCredentials credentials = new BasicAWSCredentials(
//                "ASIAYZ3LM7D2YEPFX346",
//                "hP+TXyzGi7wGBWtVOF+JUQB8ooiOHtsJdCaqfsvD"
//        );
//        AmazonS3 s3client = AmazonS3ClientBuilder
//                .standard()
//                .withCredentials(new AWSStaticCredentialsProvider(credentials))
//                .withRegion(Regions.US_EAST_1)
//                .build();
//        InputStream in = s3client.getObject("junda-cs6240", "input_test").getObjectContent();
//        File tmp = File.createTempFile("test", "");
//        Files.copy(in, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
////        File dir = new File(args[1]);
////        File dir = new File("s3://junda-cs6240/input_test");
//        for(File f : tmp.listFiles()) {
////            if(f.toPath().toString().substring(0, cache_file_prefix.length()).equals(cache_file_prefix)) {
////                job.addCacheFile(f.toURI());
////            }
//            job.addCacheFile(f.toURI());
//        }
//        File dir = new File("s3://junda-cs6240/input_test/test-r-00000");
//        job.addCacheFile(dir.toURI());
        job.addCacheFile(new Path("s3://junda-cs6240/input_test/test-r-00000").toUri());
//        job.addCacheFile(new Path("input_test/test-r-00000").toUri());

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
        if (args.length != 3) {
            throw new Error("Two arguments required:\n<input-dir> <output-dir>");
        }
        try {
            ToolRunner.run(new KNNPredictionDriver(), args);
        } catch (final Exception e) {
            logger.error("", e);
        }
    }
}
