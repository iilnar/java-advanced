package ru.ifmo.ctddev.sabirzyanov.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Created by Ilnar Sabirzyanov on 29.02.2016.
 */
public class RecursiveWalk {
    public static void println(String s, BufferedWriter out) {
        try {
            out.write(s + '\n');
        } catch (IOException e) {
            System.err.println("Couldn't write to file.");
        }
    }


    public static void walk(Path path, BufferedWriter out) {
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
                    println(Walk.getHash(file) + " " + file, out);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException e) {
                    println(Walk.ERROR_HASH + " " + file, out);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            println(Walk.ERROR_HASH + " " + path, out);
        }
    }

    public static void main(String[] args) {
        assert args != null;
        assert args.length == 2;
        try (BufferedReader in = Files.newBufferedReader(Paths.get(args[0]), Charset.forName("UTF-8"));
             BufferedWriter out = Files.newBufferedWriter(Paths.get(args[1]), Charset.forName("UTF-8"))) {
            String file;
            while ((file = in.readLine()) != null) {
                walk(Paths.get(file), out);
            }
        } catch (IOException e) {
            System.err.println("Error during file reading.");
            e.printStackTrace();
        }
    }
}
