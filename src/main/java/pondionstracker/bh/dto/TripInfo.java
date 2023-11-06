package pondionstracker.bh.dto;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pondionstracker.base.model.Trip;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TripInfo {

	private String tripId;
	private String tripHeadsign;
	private String vehicleId;

	@JsonFormat(pattern = "HH:mm:ss")
	private Date tripExpectedDepartureTime;
	private Date tripRealDepartureTime;

	@JsonFormat(pattern = "HH:mm:ss")
	private Date tripExpectedArrivalTime;
	private Date tripRealArrivalTime;
	
	private List<BusStopInfo> busStops;
	
	
	public TripInfo(Trip t) {
		this.tripId = t.getTripId();
		this.tripHeadsign = t.getTripHeadsign();
		
		var realTimeTrip = t.getRealTimeTrip();
		this.vehicleId = realTimeTrip.getIdVehicle();
		
		this.tripExpectedDepartureTime = t.getTripDepartureTime();
		this.tripRealDepartureTime = realTimeTrip.getDepartureTime();
		
		this.tripExpectedArrivalTime = t.getTripArrivalTime();
		this.tripRealArrivalTime = realTimeTrip.getArrivalTime();
		
		this.busStops = t.getBusStopsSequence()
				.stream()
				.map(BusStopInfo::new)
				.toList();
		
	}

}
