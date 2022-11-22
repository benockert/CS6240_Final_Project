package neu.cs6240.data_processing;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;

public class LyricInputMapper extends Mapper<LongWritable, Text, Text, Text> {

    private static String separator;

    @Override
    public void setup(Context context) {
        Configuration configuration = context.getConfiguration();
        separator = configuration.get("mapreduce.input.lyricsfile.separator");
    }

    @Override
    public void map(final LongWritable offset, final Text lineText, final Mapper.Context context) throws IOException, InterruptedException {
        String line = lineText.toString();
        String[] lineData = line.split(separator);
        System.out.println(lineData[0]);
    }
}