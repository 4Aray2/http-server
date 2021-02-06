package com.company;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Handler extends Thread{

    private static final Map<String, String> CONTENT_TYPES = new HashMap<>() {{
        put("json", "application/json");
    }};

    private static final String NOT_FOUND_MESSAGE = "NOT FOUND";

    private Socket socket;

    Handler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (var input = this.socket.getInputStream();
             var output = this.socket.getOutputStream()) {
            String urlString = this.getUrl(input);
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("accept", "application/json");
            InputStream responseStream = connection.getInputStream();
            var reader = new Scanner(responseStream).useDelimiter("\r\n");
            String json = reader.next();
            if (json.split("\"cod\":")[1].equals("200}")) {
                var type = CONTENT_TYPES.get("json");
                var fileBytes = json.getBytes();
                this.sendHeader(output, 200, "OK", type, fileBytes.length);
                output.write(fileBytes);
            } else {
                var type = CONTENT_TYPES.get("text");
                this.sendHeader(output, 404, "Not Found", type, NOT_FOUND_MESSAGE.length());
                output.write(NOT_FOUND_MESSAGE.getBytes());
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private String getUrl(InputStream input) {
        String a = "";
        String cityName = this.getCityName(input);
        if (this.isCity(cityName)) {
            a = "http://api.openweathermap.org/data/2.5/weather?q="
                    + cityName + "&appid=294f6aa11f38347a8146ac558e8767fa";
        } else {
            a = "http://api.openweathermap.org/data/2.5/weather?zip=" +
                    cityName + "&appid=294f6aa11f38347a8146ac558e8767fa";
        }
        return a;
    }

    private boolean isCity(String input) {
        try {
            Integer.parseInt( input.split(",")[0] );
            return false;
        }
        catch( NumberFormatException e ) {
            return true;
        }
    }

    private String getCityName(InputStream input) {
        var reader = new Scanner(input).useDelimiter("\r\n");
        var line = reader.next();
        String city = line.split(" ")[1].substring(1);
        return city;
    }

    private void sendHeader(OutputStream output, int statusCode, String statusText, String type, long length) {
        var ps = new PrintStream(output);
        ps.printf("HTTP/1.1 %s %s%n", statusCode, statusText);
        ps.printf("Content-Type: %s%n", type);
        ps.printf("Content-Length: %s%n%n", length);
    }
}
