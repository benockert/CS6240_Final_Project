package neu.cs6240.knn_prediction;

import javafx.util.Pair;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class KNNPredictionReducer extends Reducer<Text, Text, Text, Text> {
    private static final int K = 10;

    @Override
    public void reduce(final Text key, final Iterable<Text> values, final Context context) throws IOException, InterruptedException {
        //Key should be each Test Record's TrackID + Genre
        String[] keySplit = key.toString().split(",");
        String trackID = keySplit[0];
        String testGenre = keySplit[1];

        //create MaxHeap to keep track of the K closest Genre's to this Test Record key
        PriorityQueue<Pair<Double, String>> pq = new PriorityQueue<>((p1, p2) -> Double.compare(p2.getKey(), p1.getKey()));
        for (Text t : values) {
            String[] parts = t.toString().split(",");
            Double dist = Double.parseDouble(parts[0]);
            String genre = parts[1];

            pq.add(new Pair(dist, genre));

            if(pq.size() > K) {
                pq.poll();
            }
        }

        //find which genre is repeated the most in the K nearest neighbors
        HashMap<String, Integer> genreMap = new HashMap<>();
        int maxGenreCount = 0;
        String maxGenre = null;
        while(!pq.isEmpty()) {
            Pair<Double, String> p = pq.poll();
            genreMap.put(p.getValue(), genreMap.getOrDefault(p.getValue(), 0) + 1);
            if(genreMap.get(p.getValue()) > maxGenreCount) {
                maxGenreCount = genreMap.get(p.getValue());
                maxGenre = p.getValue();
            }
        }

        //update counters
        context.getCounter(KNNPredictionDriver.AccuracyCounters.TEST_RECORDS).increment(1);
        if(maxGenre.equals(testGenre)) {
            context.getCounter(KNNPredictionDriver.AccuracyCounters.CORRECT_PREDICTION).increment(1);
        }

        //write to context
        context.write(new Text(trackID), new Text(maxGenre));
    }
}