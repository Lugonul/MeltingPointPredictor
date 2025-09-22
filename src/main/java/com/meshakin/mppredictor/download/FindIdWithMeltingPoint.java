package com.meshakin.mppredictor.download;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/// Использовал необходимость написания документации для тренировки английского, прошу не судить строго.
/// Not all compounds in pubchem(chemical database) has melting point, the class find them and write their id(cid) and melting point in csv file.
public class FindIdWithMeltingPoint {
    private static final String BASE_URL = "https://pubchem.ncbi.nlm.nih.gov/rest/pug_view/annotations/heading/Melting%20Point/JSON?heading_type=Compound";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {

        try (FileWriter csvWriter = new FileWriter("./cvs_files/cid_and_melting_point_from_pubchem.cvs")) {
            csvWriter.append("cid,mp\n");

            JsonNode firsPage = getPage(1);
            JsonNode annotations = firsPage.path("Annotations");
            int totalPages = annotations.path("TotalPages").asInt();
            System.out.println("Total pages: " + totalPages);

            for (int page = 1; page <= totalPages; page++) {
                System.out.println("Processing page " + page + " of " + totalPages);

                JsonNode pageData = getPage(page);
                processPage(pageData, csvWriter);

                Thread.sleep(500);
            }

            csvWriter.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static JsonNode getPage(int pageNumber) throws Exception {
        String urlStr = BASE_URL + "&page=" + pageNumber;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlStr))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP error code: " + response.statusCode());
        }

        return objectMapper.readTree(response.body());
    }

    private static void processPage(JsonNode pageData, FileWriter csvWriter) throws Exception {
        JsonNode annotations = pageData
                .path("Annotations")
                .path("Annotation");


        for (JsonNode annotation : annotations) {
            String cid = extractCID(annotation);
            String mp = extractMeltingPoint(annotation);

            if (mp == null || mp.isEmpty()) {
                continue;
            }

            csvWriter.append(cid).append(",");
            csvWriter.append("\"").append(mp.replace("\"", "\"\"")).append("\"").append("\n");
        }
    }

    private static String extractCID(JsonNode annotation) {
        if (!annotation.has("LinkedRecords")) {
            return "N/A";
        }

        JsonNode linkedRecords = annotation.path("LinkedRecords");
        if (!linkedRecords.has("CID")) {
            return "N/A";
        }

        JsonNode cidArray = linkedRecords.path("CID");

        if (cidArray.isArray() && !cidArray.isEmpty()) {
            JsonNode firstCid = cidArray.get(0);

            if (firstCid.isObject()) {
                return firstCid.path("id").asText("N/A");
            } else {
                return firstCid.asText("N/A");
            }
        }
        return "N/A";
    }

    private static String extractMeltingPoint(JsonNode annotation) {
        if (!annotation.has("Data")) {
            return null;
        }

        JsonNode dataArray = annotation.path("Data");

        for (JsonNode data : dataArray) {
            if (!data.has("Value")) {
                continue;
            }

            JsonNode value = data.path("Value");

            if (value.has("StringWithMarkup")) {
                JsonNode stringWithMarkup = value.path("StringWithMarkup");

                if (stringWithMarkup.isArray() && !stringWithMarkup.isEmpty()) {
                    JsonNode firstMarkup = stringWithMarkup.get(0);
                    if (firstMarkup.has("String")) {
                        return firstMarkup.path("String").asText();
                    }
                }
            } else if (value.has("Number")) {
                JsonNode numberArray = value.path("Number");

                if (numberArray.isArray() && !numberArray.isEmpty()) {
                    StringBuilder numberStr = new StringBuilder();

                    for (int i = 0; i < numberArray.size(); i++) {
                        if (i > 0) {
                            numberStr.append("-");
                        }
                        JsonNode numberNode = numberArray.get(i);
                        numberStr.append(numberNode.asText());
                    }

                    if (value.has("Unit")) {
                        JsonNode unitNode = value.path("Unit");
                        String unitStr;

                        if (unitNode.isArray() && !unitNode.isEmpty()) {
                            unitStr = unitNode.get(0).asText();
                        } else {
                            unitStr = unitNode.asText();
                        }

                        return numberStr + " " + unitStr;
                    } else {
                        return numberStr.toString();
                    }
                }
            }
        }
        return null;
    }
}
