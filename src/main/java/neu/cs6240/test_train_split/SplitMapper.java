package neu.cs6240.test_train_split;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;

import java.util.Random;

import java.io.IOException;

public class SplitMapper extends org.apache.hadoop.mapreduce.Mapper<LongWritable, Text, Text, NullWritable> {
    private Random rand = new Random();
    private Double samplePercent = 0.02;
    private MultipleOutputs mos;

    @Override
    public void setup(Context context) {
        mos = new MultipleOutputs(context);
    }

    @Override
    public void map(final LongWritable offset, final Text lineText, final Context context) throws IOException, InterruptedException {
        if(rand.nextDouble() < samplePercent) {
            mos.write("test", lineText, NullWritable.get());
        } else {
            mos.write("train", lineText, NullWritable.get());
        }
    }

    @Override
    public void cleanup(Context context) throws IOException, InterruptedException {
        mos.close();
    }
}