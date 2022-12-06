package neu.cs6240.data_processing;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.*;

public class DataProcessingReducer extends Reducer<Text, Text, Text, Text> {
    private static String inputvaluesseparator;
    private static String outputseparator;
    private static String colon;

    @Override
    public void setup(Context context) {
        Configuration configuration = context.getConfiguration();
        inputvaluesseparator = configuration.get("mapreduce.reduce.inputvalues.separator");
        outputseparator = configuration.get("mapreduce.output.textoutputformat.separator");
        colon = configuration.get("mapreduce.input.lyricsindex.separator");
    }

    @Override
    public void reduce(final Text key, final Iterable<Text> values, final Context context) throws IOException, InterruptedException {
        Map<Integer, String> h = new HashMap<>();
        Text genre = null;

        for (Text t : values) {
            String[] parts = t.toString().split(inputvaluesseparator);
            String lyricOrGenre = parts[0];

            if (lyricOrGenre.equals("L")) {
                String[] indexCount = parts[1].split(colon);
                Integer index = Integer.parseInt(indexCount[0].toString());
                String count = indexCount[1].toString();
                h.put(index, count);
            }
            else if (lyricOrGenre.equals("G")) {
                genre = new Text(parts[1]);
            }
        }

        // only if there is genre and lyric data for a track
        if (genre != null && h.entrySet().size() > 0) {
            StringBuilder sb = new StringBuilder(genre + outputseparator);
            for (int i =0; i<5000; i++) {
                sb.append(h.getOrDefault(i, ""));
                if (i<4999) {
                    sb.append(outputseparator);
                }
            }

            context.write(key, new Text(sb.toString()));
        }
    }
}