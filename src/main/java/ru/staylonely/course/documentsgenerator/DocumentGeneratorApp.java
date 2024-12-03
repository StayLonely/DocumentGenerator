package ru.staylonely.course.documentsgenerator;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.json.JSONObject;

import java.io.*;

public class DocumentGeneratorApp extends Application {

    private TextArea resultTextArea;
    private TextField topicInput;

    @Override
    public void start(Stage stage) {
        // Настройка интерфейса
        stage.setTitle("Document Generator");

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(15));
        vbox.setStyle("-fx-background-color: #f0f0f0;");

        // Поле для ввода темы
        topicInput = new TextField();
        topicInput.setPromptText("Введите тему документа...");
        topicInput.setStyle("-fx-border-color: #007bff; -fx-background-color: white;");

        Label uniquenessLabel = new Label("Выбор уникальности:");
        uniquenessLabel.setStyle("-fx-font-weight: bold;"); // Добавляем жирный шрифт для выделения


        // Создаем слайдер
        Slider temperatureSlider = new Slider(0, 1, 0.5); // Диапазон от 0 до 1, начальное значение 0.5
        temperatureSlider.setShowTickMarks(true);
        temperatureSlider.setShowTickLabels(true);
        temperatureSlider.setMajorTickUnit(0.1);
        temperatureSlider.setMinorTickCount(5);
        temperatureSlider.setBlockIncrement(0.1);

        // Создаем метку для отображения значения слайдера
        Label temperatureLabel = new Label("Температура (уникальность): 0.5");

        // Обрабатываем изменение значения слайдера
        temperatureSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            temperatureLabel.setText(String.format("Температура (уникальность): %.2f", newValue.doubleValue()));
        });


        // Кнопка для генерации документа
        Button generateButton = new Button("Сгенерировать документ");
        generateButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white;");
        generateButton.setOnAction(e -> generateDocument(temperatureSlider.getValue()));

        // Область для результата
        resultTextArea = new TextArea();
        resultTextArea.setEditable(true);
        resultTextArea.setWrapText(true);
        resultTextArea.setStyle("-fx-border-color: #007bff; -fx-background-color: white;");

        // Кнопка для сохранения документа
        Button saveButton = new Button("Сохранить документ");
        saveButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white;");
        saveButton.setOnAction(e -> saveDocument());

        // Добавляем элементы в интерфейс
        vbox.getChildren().addAll(topicInput, uniquenessLabel, temperatureSlider, temperatureLabel, generateButton, resultTextArea, saveButton);


        Scene scene = new Scene(vbox, 400, 300);
        stage.setScene(scene);
        stage.show();
    }

    private void generateDocument(Double temperature) {
        String topic = topicInput.getText().trim();
        if (topic.isEmpty()) {
            showAlert("Ошибка", "Пожалуйста, введите тему документа.");
            return;
        }
        String temperatureSlider = temperature.toString();
        if (temperatureSlider.isEmpty()) {
            showAlert("Ошибка", "Пожалуйста, выберите значение уникальности документа.");
            return;
        }

        ApiClient apiClient = new ApiClient();
        String url = "https://llm.api.cloud.yandex.net/foundationModels/v1/completion";
        String modelUri = "gpt://b1g7js0hl687pb9ucuke/yandexgpt/latest";

        // Тема документа от пользователя
        String userTopic = "Контракт на разработку программного обеспечения";
        String jsonResponse = "";
        try {
            // Генерируем документ
            jsonResponse = apiClient.generateDocument(url, modelUri, userTopic, temperatureSlider);
            // Выводим ответ
            System.out.println("Response: " + jsonResponse);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            jsonResponse = e.getMessage().toString();
        }
        JSONObject jsonObject = new JSONObject(jsonResponse);
        String messageText = jsonObject.getJSONObject("result")
                .getJSONArray("alternatives")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("text");

        // Устанавливаем текст в текстовую область
        resultTextArea.setText(messageText);

        //автоматизировать n документов
        //можно ли скормить референс ?
        // структура документа (печать например) чтобы выглядил как документ
        //сложные структуры

    }

    private void saveDocument() {
        String content = resultTextArea.getText();
        if (content.isEmpty()) {
            showAlert("Ошибка", "Нет текста для сохранения.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить документ");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Word Document", "*.docx"));
        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try (XWPFDocument document = new XWPFDocument()) {
                // Разделяем текст на части по тегу <n>
                String[] sections = content.split("<n>");

                for (String section : sections) {
                    section = section.trim();

                    // Обрабатываем заголовок H2
                    if (section.startsWith("<h2>")) {
                        String headerText = section.substring(4, section.indexOf("</h2>")).trim();
                        processHeader(document, headerText);
                    } else {
                        // Убираем теги <p>, <left>, <right>, <mid>, <br> и оставляем текст
                        section = section.replaceAll("<p>", "").replaceAll("</p>", "");
                        //section = section.replaceAll("<left>", "").replaceAll("<right>", "").replaceAll("<mid>", "");
                        //section = section.replaceAll("</left>", "").replaceAll("</right>", "").replaceAll("</mid>", "");
                        section = section.replaceAll("<br>", "\n");
                        section = section.replaceAll("</n>", "").replaceAll("</n>", "");
                        section = section.replaceAll("<left>", "").replaceAll("</left>", "").trim();

                       if (section.contains("<right>")) {
                           section = section.replaceAll("<left>", "").replaceAll("</left>", "").trim();

                           String rightText = section.replaceAll("<right>", "").replaceAll("</right>", "").trim();
                           processParagraph(document, rightText, ParagraphAlignment.RIGHT);
                        } else if(section.contains("<mid>")) {
                            String midText = section.replaceAll("<mid>", "").replaceAll("</mid>", "");
                            processParagraph(document, midText, ParagraphAlignment.CENTER);
                        }
                        else {
                            processParagraph(document, section, ParagraphAlignment.LEFT);
                        }


                    }
                }

                // Сохранение документа в формате .docx
                try (FileOutputStream out = new FileOutputStream(file)) {
                    document.write(out);
                }

                showAlert("Успех", "Документ успешно сохранён.");
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Ошибка", "Не удалось сохранить файл.");
            }
        }
    }

    // Пример обработки заголовка
    private void processHeader(XWPFDocument document, String headerText) {
        XWPFParagraph heading = document.createParagraph();
        XWPFRun run = heading.createRun();
        run.setBold(true); // Заголовок жирным
        run.setText(headerText);
        heading.setAlignment(ParagraphAlignment.CENTER); // Центрирование заголовка
    }

    // Обработка параграфов с указанием выравнивания
    private void processParagraph(XWPFDocument document, String text, ParagraphAlignment alignment) {
        if (text.isEmpty()) return; // Пропускаем пустые строки

        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(alignment); // Устанавливаем выравнивание

        // Создание нового объекта XWPFRun для добавления текста
        XWPFRun run = paragraph.createRun();
        run.setText(text);
    }





    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch();
    }
}