package ru.ifmo.ctddev.sabirzyanov.walk;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Ilnar Sabirzyanov on 29.02.2016.
 */
public class Walk {
    private static HexBinaryAdapter adapter = new HexBinaryAdapter();
    static String ERROR_HASH = adapter.marshal(new byte[16]);

    public static String getHash(Path file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            InputStream is = Files.newInputStream(file);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                md.update(buffer, 0, len);
            }
            return adapter.marshal(md.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            return ERROR_HASH;
        }
    }
}