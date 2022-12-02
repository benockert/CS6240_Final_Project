package neu.cs6240.data_processing;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;

import java.io.IOException;
import java.util.*;

public class DataProcessingReducer extends Reducer<Text, Text, Text, Text> {
    private static String inputvaluesseparator;
    private static String outputseparator;
    private static String lyriccountseparator;
    private static Integer outputLength;
    private Random rand;
    private Double samplePercent;
    private MultipleOutputs mos;

    @Override
    public void setup(Context context) {
        Configuration configuration = context.getConfiguration();
        inputvaluesseparator = configuration.get("mapreduce.reduce.inputvalues.separator");
        outputseparator = configuration.get("mapreduce.output.textoutputformat.separator");
        lyriccountseparator = configuration.get("mapreduce.input.lyricsindex.separator");
        outputLength = 5000;
        rand = new Random();
        samplePercent = 0.1;
        mos = new MultipleOutputs(context);
    }

    @Override
    public void reduce(final Text key, final Iterable<Text> values, final Context context) throws IOException, InterruptedException {
        Map<Integer, Integer> h = new HashMap<>();
        Text genre = null;


        for (Text t : values) {

            if(rand.nextDouble() < samplePercent) {
                mos.write("test", key, t);
            } else {
                mos.write("train", key, t);
            }
        }
//            String[] parts = t.toString().split(inputvaluesseparator);
//            String lyricOrGenre = parts[0];
//
//            if (lyricOrGenre.equals("L")) {
//                String[] lyricCountEntry = parts[1].split(lyriccountseparator);
//                Integer lyricIndex = Integer.parseInt(lyricCountEntry[0]);
//                Integer lyricCountOccurrences = Integer.parseInt(lyricCountEntry[1]);
//                h.put(lyricIndex, lyricCountOccurrences);
//            }
//            else if (lyricOrGenre.equals("G")) {
//                genre = new Text(parts[1]);
//            }
//        }
//
//        // only if there is genre and lyric data for a track
//        if (genre != null && h.entrySet().size() > 0) {
//            StringBuilder sb = new StringBuilder();
//            sb.append(genre).append(outputseparator);
//            for (int i =0; i<outputLength; i++) {
//                sb.append(h.getOrDefault(i,0));
//                if (i<outputLength - 1) {
//                    sb.append(outputseparator);
//                }
//
//                if(rand.nextDouble() < samplePercent) {
//                    mos.write("test", key, new Text(sb.toString()));
//                } else {
//                    mos.write("train", key, new Text(sb.toString()));
//                }
//            }
//        }
    }

    @Override
    public void cleanup(Context context) throws IOException, InterruptedException {
        mos.close();
    }
}