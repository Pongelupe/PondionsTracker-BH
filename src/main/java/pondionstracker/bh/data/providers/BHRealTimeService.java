package pondionstracker.bh.data.providers;

import java.util.List;
import java.util.Map;

import pondionstracker.data.components.QueryExecutor;
import pondionstracker.data.constants.Query.Parameter;
import pondionstracker.data.providers.impl.DefaultRealTimeService;

public class BHRealTimeService extends DefaultRealTimeService {
	
	private static final String FILENAME_QUERY_ROUTE_TO_ID_LINE = "ROUTE_TO_ID_LINE";

	public BHRealTimeService(QueryExecutor queryExecutor) {
		super(queryExecutor);
	}
	
	@Override
	public List<String> getIdsLineByRouteId(String routeId) {
		return queryExecutor.queryAll(FILENAME_QUERY_ROUTE_TO_ID_LINE, 
				rs -> rs.getString(1), Map.of(Parameter.ROUTE_ID, routeId));
	}

}
