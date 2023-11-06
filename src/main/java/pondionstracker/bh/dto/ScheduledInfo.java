package pondionstracker.bh.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledInfo {
	
	private String routeShortName;
	
	private long totalEntries;

	private long totalTrips;

	private long collectedTrips;
	
	private long matchedTrips;
	
	private List<TripInfo> trips;
	
	
	public double getPercentageMatched() {
		return (double) matchedTrips / totalTrips;
	}
	
	
}
