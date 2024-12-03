package ru.staylonely.course.documentsgenerator;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


public class ApiClient {

    public String generateDocument(String url, String modelUri, String topic, String temperature) throws IOException, InterruptedException {
        // Формируем текст запроса на основе темы пользователя
        String prompt = topic.isEmpty() ?
                "Сгенерируй какой-нибудь официальный документ который мог бы использоваться в любой компании на твой выбор, текст должен быть форматирован, строки должны быть внутри тега, <br> - красная строка, <n> перенос строки, <left> - текст слева, <right> - текст справа, <mid> текст по центру" :
                "Сгенерируй официальный документ, который мог бы использоваться в компании - " + topic + ", текст должен быть форматирован, строки должны быть внутри тега, <br> - красная строка, <n> перенос строки, <left> - текст слева, <right> - текст справа, <mid> текст по центру";

        // Создаем JSON-объект для запроса
        String jsonRequest = "{" +
                "\"modelUri\": \"" + modelUri + "\"," +
                "\"completionOptions\": {" +
                "\"stream\": false," +
                "\"temperature\":" + temperature + "," + // Динамическое использование temperature
                "\"maxTokens\": \"2000\"" +
                "}," +
                "\"messages\": [" +
                "{" +
                "\"role\": \"system\"," +
                "\"text\": \"Ты — человек, который отвечает за различные документы внутри компании\"" +
                "}," +
                "{" +
                "\"role\": \"user\"," +
                "\"text\": \"" + prompt + "\"" +
                "}" +
                "]" +
                "}";


    HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json") // Первый заголовок
                .header("Authorization", "Api-Key AQVNz88mtOMDbGAh_RoIk89B2OC4ujjANhyk1jli") // Второй заголовок, например, токен авторизации
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();


        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new IOException("Unexpected response code: " + response.statusCode());
        }
    }

}