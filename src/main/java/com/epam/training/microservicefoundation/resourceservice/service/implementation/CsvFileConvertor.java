package com.epam.training.microservicefoundation.resourceservice.service.implementation;

import com.epam.training.microservicefoundation.resourceservice.service.Convertor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
@Component
public class CsvFileConvertor implements Convertor<List<Long>, MultipartFile> {
    private static final Logger log = LoggerFactory.getLogger(CsvFileConvertor.class);
    private static final int ID_START_INDEX = 3;
    private static final String SPLITTER = ",";
    private static final String STARTER = "id=";

    @Override
    public List<Long> covert(MultipartFile input) {
        log.info("Converting multipart file '{}' to list of ids", input.getName());
        try(BufferedInputStream bufferedInputStream = new BufferedInputStream(input.getInputStream())) {
            int character;
            StringBuilder stringBuilder = new StringBuilder();
            while ((character = bufferedInputStream.read()) != -1) {
                stringBuilder.append((char)character);
            }
            return getIds(stringBuilder.toString());
        } catch (IOException ex) {
            log.error("CSV file conversion failed", ex);
            throw new IllegalStateException("CSV file reading process failed", ex);
        }
    }

    private List<Long> getIds(String text) {
        if(!text.startsWith(STARTER)) {
            IllegalArgumentException illegalArgumentException =
                    new IllegalArgumentException(String.format("Passed argument list is not started with '%s'", STARTER));

            log.error("File content format is not valid", illegalArgumentException);
            throw illegalArgumentException;
        }

        String allIds = text.substring(ID_START_INDEX);  // ids after '=', i.e 1,2,3,...
        return Arrays.stream(allIds.split(SPLITTER))
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }
}
