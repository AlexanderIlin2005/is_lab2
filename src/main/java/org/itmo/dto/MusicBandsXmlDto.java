package org.itmo.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class MusicBandsXmlDto {

    // useWrapping = false prevents an extra <musicBands><musicBands> layer
    @JacksonXmlElementWrapper(useWrapping = false)
    // Maps the XML element <musicBand> to items in this list
    @JacksonXmlProperty(localName = "musicBand")
    private List<MusicBandCreateDto> musicBands;
}