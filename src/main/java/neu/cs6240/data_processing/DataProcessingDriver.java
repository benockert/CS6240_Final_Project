package neu.cs6240.data_processing;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class DataProcessingDriver extends Configured implements Tool {
    private static final Logger logger = LogManager.getLogger(DataProcessingDriver.class);

    public int run(final String[] args) throws Exception {
        final Configuration conf = getConf();
        final Job job = Job.getInstance(conf, "CS6240 Final Project Data Processor");
        job.setJarByClass(DataProcessingDriver.class);
        final Configuration jobConf = job.getConfiguration();

        jobConf.set("mapreduce.input.lyricsfile.separator", ",");
        jobConf.set("mapreduce.input.lyricsindex.separator", ":");
        jobConf.set("mapreduce.input.genresfile.separator", "\t");
        jobConf.set("mapreduce.reduce.inputvalues.separator", "-");
        jobConf.set("mapreduce.output.textoutputformat.separator", ",");

        MultipleInputs.addInputPath(job, new Path(args[0]), TextInputFormat.class, LyricInputMapper.class);
        MultipleInputs.addInputPath(job, new Path(args[1]), TextInputFormat.class, GenreInputMapper.class);

        job.setReducerClass(DataProcessingReducer.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        MultipleOutputs.addNamedOutput(job, "test", TextOutputFormat.class, Text.class, Text.class);
        MultipleOutputs.addNamedOutput(job, "train", TextOutputFormat.class, Text.class, Text.class);
        FileOutputFormat.setOutputPath(job, new Path(args[2]));

        int res = job.waitForCompletion(true) ? 0 : 1;
        return res;
    }

    public static void main(final String[] args) {
        if (args.length != 3) {
            throw new Error("Three arguments required:\n<lyric-input-dir> <genre-input-dir> <output-dir>");
        }

        try {
            ToolRunner.run(new DataProcessingDriver(), args);
        } catch (final Exception e) {
            logger.error("", e);
        }
    }
}