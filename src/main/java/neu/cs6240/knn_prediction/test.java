package neu.cs6240.knn_prediction;

import java.io.*;
import java.net.URI;
import java.util.PriorityQueue;
import java.util.zip.GZIPInputStream;

public class test {
    public static void main(String[] args) throws IOException {
        File dir = new File("input_test/");
//        File dir = new File("input_test/");
        for(File f : dir.listFiles()) {
            String cache_file_prefix = "input_test/test";
            if(f.toPath().toString().substring(0, cache_file_prefix.length()).equals(cache_file_prefix)) {
                System.out.println(f.toPath());
            }
        }
//        File[] list = dir.listFiles();
//        URI[] files = new URI[list.length];
//        for(int i = 0; i < list.length; i++) {
//            files[i] = list[i].toURI();
//        }
//
//        for(URI f : files) {
//            try {
//                String filepath = f.toString();
//                File file = new File(f);
//                BufferedReader rdr = new BufferedReader(new FileReader(file));
//                String line;
//                while((line = rdr.readLine()) != null) {
//                    String[] attr = line.split(",");
//
//                    //for each test record, create a max ordered Priority queue of distances. we will only maintain the K smallest distance nodes
//                    //attr[0] == unique TrackID from Test File
//                    if(attr.length != 5002) {
//                        System.out.println(filepath + " : " + attr[0] + ": " + attr.length);
//                    }
//                }
//            } catch (IOException e) {
//                System.out.println("failed to read");
//            }
//
//        }
    }
}
