package mg.mgmap.generic.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.NameUtil;

public class SHA256 {

    public static void main(String[] args) {
        System.out.println("Hello from SHA256 " + args.length);
        for (String arg : args ){
            System.out.println(arg);
        }
        try {
            if (args.length >= 1){
                File f = new File(args[0]);
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                for (File child : f.listFiles()){
                    if (child.getName().endsWith(".apk")){

                        String hash = getFileChecksum(md, child);
                        System.out.println(f.getAbsolutePath()+" "+child.getName()+" "+hash);
                        File f2 = new File(f, child.getName()+".sha256");
                        PrintWriter pw = new PrintWriter(f2);
                        pw.println(child.getName()+"="+hash);
                        pw.close();

                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean verify(File apkFile){
        boolean res = false;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            if (apkFile.exists()) {
                File f2 = new File(apkFile.getParentFile(), apkFile.getName() + ".sha256");
                if (f2.exists()) {
                    String line = new BufferedReader(new FileReader(f2)).readLine();
                    String hash = getFileChecksum(md, apkFile);

                    Log.i(MGMapApplication.LABEL, NameUtil.context()+"\n calcSHA="+hash+"\ntransSHA="+line.replaceFirst(".*=",""));
                    if (hash.equals(line.replaceFirst(".*=", ""))) {
                        res = true;
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    private static String getFileChecksum(MessageDigest digest, File file) throws IOException
    {
        //Get file input stream for reading the file content
        FileInputStream fis = new FileInputStream(file);

        //Create byte array to read data in chunks
        byte[] byteArray = new byte[1024];
        int bytesCount;

        //Read file data and update in message digest
        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        }

        //close the stream; We don't need it now.
        fis.close();

        //Get the hash's bytes
        byte[] bytes = digest.digest();

        //This bytes[] has bytes in decimal format;
        //Convert it to hexadecimal format
        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
        }

        //return complete hash
        return sb.toString();
    }
}