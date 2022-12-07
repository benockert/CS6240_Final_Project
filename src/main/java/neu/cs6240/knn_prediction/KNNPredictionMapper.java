package neu.cs6240.knn_prediction;

import javafx.util.Pair;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class KNNPredictionMapper extends Mapper<LongWritable, Text, Text, Text> {
    private ArrayList<String> testFile;
    private HashMap<String, PriorityQueue<Pair<Double, String>>> map;
    private final static int K = 10;
    private final static double divisor = 30.0;

    @Override
    public void setup(Context context) throws IOException, InterruptedException {
        testFile = new ArrayList<>();
        map = new HashMap<>();
        URI[] files = context.getCacheFiles();
        for(URI f : files) {
            BufferedReader rdr = new BufferedReader(
                    new InputStreamReader(
                            new GZIPInputStream(
                                    new FileInputStream(
                                            new File(f.toString())
                                    )
                            )
                    )
            );
            String line;
            while((line = rdr.readLine()) != null) {
                testFile.add(line);
                String[] attr = line.split(",");

                //for each test record, create a max ordered Priority queue of distances. we will only maintain the K smallest distance nodes
                //attr[0] == unique TrackID from Test File
                map.put(attr[0] + "," + attr[1], new PriorityQueue<>((p1, p2) -> Double.compare(p2.getKey(), p1.getKey())));
            }
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
        List<String> train = Arrays.asList(trainRecord.split(","));
        List<String> test = Arrays.asList(testRecord.split(","));

        double distance = 0.0;
        //lyric attributes begin at position 2
        //train and test size <= 22
        for(int i = 2; i < Math.max(train.size(), test.size()); i++) {
            if(i > train.size() || i > test.size() || !train.contains(test.get(i))) {
                distance += 1.0;
            } else if(train.get(i).equals(test.get(i))) {
                distance += 0.0;
            } else if(train.contains(test.get(i))) {
                // divide by 30 to ensure that the add value is less than 1. Assumes that records have at most 20 lyrics
                distance += Math.abs(train.indexOf(test.get(i)) - i) / divisor;
            }
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
                //key == distance, value == genre
                newValue.set(p.getKey() + "," + p.getValue());
                context.write(newKey, newValue);
            }
        }
    }
}
