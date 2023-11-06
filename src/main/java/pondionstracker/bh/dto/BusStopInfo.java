package pondionstracker.bh.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pondionstracker.base.model.BusStatusAtStop;
import pondionstracker.base.model.BusStopTrip;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class BusStopInfo {

	private int stopSequence;
	
	private String idStop;
	
	private int numberOfEntries;
	
	private boolean calculated;
	
	@JsonFormat(pattern = "HH:mm:ss")
	private Date expectedTime;
	
	private Date realTime;
	
	private Long diffInMinutes;
	
	private BusStatusAtStop status;
	
	public BusStopInfo(BusStopTrip bst) {
		this.stopSequence = bst.getStopSequence();
		this.idStop = bst.getIdStop();
		this.numberOfEntries = bst.getEntries().size();
		this.calculated = bst.isCalculated();
		
		this.expectedTime = bst.getExpectedTime();
		this.realTime = bst.getRealTime();
		this.diffInMinutes = bst.getExpectedRealTimeDiffInMinutes();
		this.status = bst.getStatus();
	}
	
}
