package com.async_alpha.api_simulator.service;

import com.async_alpha.api_simulator.model.RequestType;
import com.async_alpha.api_simulator.model.ServiceRequest;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class DatasetLoaderTest {

    @Test
    public void testParseLineStandard() {
        DatasetLoader loader = new DatasetLoader();
        ServiceRequest req = loader.parseLine("10:15:30, CLIENT_A, READ");
        assertNotNull(req);
        assertEquals("CLIENT_A", req.getClientId());
        assertEquals(RequestType.READ, req.getRequestType());
        assertEquals(10, req.getTimestamp().getHour());
        assertEquals(15, req.getTimestamp().getMinute());
        assertEquals(30, req.getTimestamp().getSecond());
    }

    @Test
    public void testParseFromText() throws IOException {
        DatasetLoader loader = new DatasetLoader();
        String text = """
                # Comment line
                10:15:30, CLIENT_A, READ
                10:15:35, CLIENT_B, WRITE
                timestamp, clientId, requestType
                10:16:00, CLIENT_C, DELETE
                """;

        DatasetLoader.LoadResult result = loader.loadFromText(text);
        assertEquals(3, result.getSuccessCount());
        assertEquals(0, result.getErrorCount());
        assertEquals("CLIENT_A", result.getRequests().get(0).getClientId());
        assertEquals("CLIENT_B", result.getRequests().get(1).getClientId());
        assertEquals("CLIENT_C", result.getRequests().get(2).getClientId());
    }

    @Test
    public void testSampleFilesExist() throws IOException {
        DatasetLoader loader = new DatasetLoader();
        File txtFile = new File("sample_data/test_requests.txt");
        assertTrue(txtFile.exists(), "test_requests.txt should exist");

        DatasetLoader.LoadResult txtResult = loader.loadFromFile(txtFile);
        assertTrue(txtResult.getSuccessCount() > 0);

        File csvFile = new File("sample_data/sample_requests.csv");
        assertTrue(csvFile.exists(), "sample_requests.csv should exist");

        DatasetLoader.LoadResult csvResult = loader.loadFromFile(csvFile);
        assertTrue(csvResult.getSuccessCount() > 0);
    }
}
