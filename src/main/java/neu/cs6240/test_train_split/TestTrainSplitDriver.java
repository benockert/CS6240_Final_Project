package neu.cs6240.test_train_split;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class TestTrainSplitDriver extends Configured implements Tool {
    private static final Logger logger = LogManager.getLogger(TestTrainSplitDriver.class);

    public int run(final String[] args) throws Exception {
        final Configuration conf = getConf();
        final Job job = Job.getInstance(conf, "CS6240 Final Project Data Train / Test Splitter");
        job.setJarByClass(TestTrainSplitDriver.class);
        final Configuration jobConf = job.getConfiguration();

        jobConf.set("mapreduce.output.textoutputformat.separator", "\t");

        job.setMapperClass(SplitMapper.class);
        job.setNumReduceTasks(0);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(NullWritable.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(NullWritable.class);
        MultipleOutputs.addNamedOutput(job, "test", TextOutputFormat.class, Text.class, NullWritable.class);
        MultipleOutputs.addNamedOutput(job, "train", TextOutputFormat.class, Text.class, NullWritable.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        int res = job.waitForCompletion(true) ? 0 : 1;
        return res;
    }

    public static void main(final String[] args) {
        if (args.length != 2) {
            throw new Error("Two arguments required:\n<input-dir> <output-dir>");
        }
        try {
            ToolRunner.run(new TestTrainSplitDriver(), args);
        } catch (final Exception e) {
            logger.error("", e);
        }
    }
}