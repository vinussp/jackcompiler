package br.ufma.ecp;

import static br.ufma.ecp.token.TokenType.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import br.ufma.ecp.token.Token;

public class App {

    public static void saveToFile(String fileName, String output) {
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(fileName);
            byte[] strToBytes = output.getBytes();
            outputStream.write(strToBytes);
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String fromFile(File file) {
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file.toPath());
            String textoDoArquivo = new String(bytes, "UTF-8");
            return textoDoArquivo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void main(String[] args) {
        // Caminho fixo para o arquivo Main.jack

        //Average
        String folderPath  = "D:\\Documentos\\IdeaProjects\\jackcompiler\\src\\test\\Average";

        //Pong
        //String folderPath = "D:\\Documentos\\IdeaProjects\\jackcompiler\\src\\test\\Pong";

        //Seven
        //String folderPath  = "D:\\Documentos\\IdeaProjects\\jackcompiler\\src\\test\\Seven\";


        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            System.err.println("The directory doesn't exist or is not a valid directory: " + folderPath);
            System.exit(1);
        }

        // Lista todos os arquivos .jack no diretório
        File[] jackFiles = folder.listFiles((dir, name) -> name.endsWith(".jack"));

        if (jackFiles == null || jackFiles.length == 0) {
            System.err.println("No .jack files found in the directory: " + folderPath);
            System.exit(1);
        }

        for (File file : jackFiles) {
            var inputFileName = file.getAbsolutePath();
            var pos = inputFileName.lastIndexOf('.');
            var outputFileName = inputFileName.substring(0, pos) + ".vm";

            System.out.println("Compiling " + inputFileName);
            var input = fromFile(file);
            var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
            parser.parse();
            var result = parser.VMOutput();
            saveToFile(outputFileName, result);

            System.out.println("Compilation completed: " + outputFileName);
        }

        System.out.println("All .jack files have been compiled.");
    }
}

