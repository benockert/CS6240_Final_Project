package neu.cs6240.data_processing;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;
import java.util.*;

public class LyricInputMapper extends Mapper<LongWritable, Text, Text, Text> {
    private final static String lyricOutputKey = "L-";

    private static String inputseparator;
    private static String lyricindexseparator;
    private static String outputseparator;

    private List<Map.Entry<Integer, Integer>> sortMapByValuesAndReturnTop(Map<Integer, Integer> h, Integer topX) {
        List<Map.Entry<Integer, Integer>> list = new LinkedList<Map.Entry<Integer, Integer>>(h.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<Integer, Integer>>() {
            public int compare(Map.Entry<Integer, Integer> i1, Map.Entry<Integer, Integer> i2) {
                // sort in descending order of lyric occurrence
                return (i2.getValue().compareTo(i1.getValue()));
            }
        });

        // return top X, or the entire list if the length is less than topX
        try {
            return list.subList(0,topX);
        }
        catch (IndexOutOfBoundsException e) {
            return list;
        }
    }

    @Override
    public void setup(Context context) {
        Configuration configuration = context.getConfiguration();
        inputseparator = configuration.get("mapreduce.input.lyricsfile.separator");
        lyricindexseparator = configuration.get("mapreduce.input.lyricsindex.separator");
        outputseparator = configuration.get("mapreduce.reduce.inputvalues.separator");
    }

    @Override
    public void map(final LongWritable offset, final Text lineText, final Context context) throws IOException, InterruptedException {
        Map<Integer, Integer> h = new HashMap<>();

        String line = lineText.toString();
        if (!line.startsWith("#") && !line.startsWith("%")) {
            String[] lineData = line.split(inputseparator);

            String trackId = lineData[0];
            for (int i=2; i<lineData.length; i++) {
                String[] lyricData = lineData[i].split(lyricindexseparator);
                Integer lyricIndex = Integer.parseInt(lyricData[0]);
                Integer occurrences = Integer.parseInt(lyricData[1]);
                h.put(lyricIndex, occurrences);
            }

            List<Map.Entry<Integer, Integer>> topLyrics = this.sortMapByValuesAndReturnTop(h, 20);
            for (int i=0; i<topLyrics.toArray().length; i++) {
                String lyricKeyAndIndex = lyricOutputKey + i + outputseparator + topLyrics.get(i).getKey();
                context.write(new Text(trackId), new Text(lyricKeyAndIndex));
            }
        }
    }
}