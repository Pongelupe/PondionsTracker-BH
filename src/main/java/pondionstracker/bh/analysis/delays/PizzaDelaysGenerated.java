package pondionstracker.bh.analysis.delays;

import static pondionstracker.base.model.BusStatusAtStop.AHEAD_OF_SCHEDULE;
import static pondionstracker.base.model.BusStatusAtStop.DELAYED;
import static pondionstracker.base.model.BusStatusAtStop.ON_TIME;
import static pondionstracker.base.model.BusStatusAtStop.UNKOWN;

import java.awt.Color;
import java.util.List;
import java.util.Map;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;
import org.knowm.xchart.PieSeries;
import org.knowm.xchart.PieSeries.PieSeriesRenderStyle;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.style.PieStyler.LabelType;

import lombok.SneakyThrows;
import pondionstracker.base.model.BusStatusAtStop;

public class PizzaDelaysGenerated {
	
	private static record SeriesInfo(String title, Color c) {}
	
	private static final Map<BusStatusAtStop, SeriesInfo> NAMES = Map.of(UNKOWN, new SeriesInfo("UNKOWN", Color.black),
			AHEAD_OF_SCHEDULE, 
			new SeriesInfo("AHEAD", new Color(166, 35, 78)),
			DELAYED, new SeriesInfo("DELAYED", new Color(50, 168, 82)),
			ON_TIME, new SeriesInfo("ON_TIME", new Color(30, 23, 230))
			);
	
	private record PizzaInfo(String title, Map<BusStatusAtStop, Integer> m) {}
	

	@SneakyThrows
	public static void main(String[] args) {
		var mapScheduledTripsWeekdays = Map.of(UNKOWN, 1292,
				BusStatusAtStop.AHEAD_OF_SCHEDULE, 453311,
				BusStatusAtStop.DELAYED, 5878585,
				BusStatusAtStop.ON_TIME, 214129);
		
		var mapGeneratedTripsWeekdays = Map.of(
				BusStatusAtStop.AHEAD_OF_SCHEDULE, 1163766,
				BusStatusAtStop.DELAYED, 5174354,
				BusStatusAtStop.ON_TIME, 209197);
		
		
		var mapScheduledTripsSaturday = Map.of(BusStatusAtStop.UNKOWN, 231,
				BusStatusAtStop.AHEAD_OF_SCHEDULE, 82827,
				BusStatusAtStop.DELAYED, 1147656,
				BusStatusAtStop.ON_TIME, 49975);
		var mapGeneratedTripsSaturday = Map.of(
				BusStatusAtStop.AHEAD_OF_SCHEDULE, 235896,
				BusStatusAtStop.DELAYED, 999427,
				BusStatusAtStop.ON_TIME, 45366);
		

		var mapScheduledTripsSunday = Map.of(BusStatusAtStop.UNKOWN, 124,
				BusStatusAtStop.DELAYED, 802080,
				BusStatusAtStop.AHEAD_OF_SCHEDULE, 47605,
				BusStatusAtStop.ON_TIME, 38801);
		var mapGeneratedTripsSunday = Map.of(
				BusStatusAtStop.AHEAD_OF_SCHEDULE, 164289,
				BusStatusAtStop.DELAYED, 690032,
				BusStatusAtStop.ON_TIME, 34289);
		
		var charts = List.of(
				new PizzaInfo("Status - Weekdays", mapScheduledTripsWeekdays),
				new PizzaInfo("Status - Weekdays Generated", mapGeneratedTripsWeekdays),
				new PizzaInfo("Status - Saturday", mapScheduledTripsSaturday),
				new PizzaInfo("Status - Saturday Generated", mapGeneratedTripsSaturday),
				new PizzaInfo("Status - Sunday", mapScheduledTripsSunday),
				new PizzaInfo("Status - Sunday Generated", mapGeneratedTripsSunday)
				)
		.stream()
		.map(info -> {
			PieChart chart = new PieChartBuilder().width(600).height(450).title(info.title).build();
			info.m.forEach((k,v) -> {
				SeriesInfo seriesInfo = NAMES.get(k);
				PieSeries s = chart.addSeries(seriesInfo.title(), v);
				s.setFillColor(seriesInfo.c);
			});
			
		    chart.getStyler().setLabelsFontColorAutomaticEnabled(false);
		    
		    
		    chart.getStyler().setLegendVisible(false);
			chart.getStyler().setSumVisible(false);
			chart.getStyler().setPlotContentSize(.85);
			chart.getStyler().setSumFontSize(2.8f);
		    chart.getStyler().setLabelsDistance(1.15);
		    chart.getStyler().setStartAngleInDegrees(90);
		    chart.getStyler().setForceAllLabelsVisible(true);
		    chart.getStyler().setLabelType(LabelType.NameAndPercentage);
		    chart.getStyler().setLabelsFontColor(Color.black);
		    chart.getStyler().setDefaultSeriesRenderStyle(PieSeriesRenderStyle.Donut);

			return chart;
		}).toList();
		
	    
		BitmapEncoder.saveBitmap(charts, 3, 2, "./charts/pizza_delays", BitmapFormat.PNG);
	    new SwingWrapper<PieChart>(charts).displayChartMatrix();
	}

}
