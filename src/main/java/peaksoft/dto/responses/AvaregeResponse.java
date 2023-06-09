package peaksoft.dto.responses;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * @author :ЛОКИ Kelsivbekov
 * @created 21.03.2023
 */
@Builder
public record AvaregeResponse(BigDecimal oneDayAveragePrice) {

}
