with route_short_name as (select concat(route_short_name, '%') as codigo_linha 
from routes r where route_id = :ROUTE_ID)
select b.id_linha from route_short_name s, bus_line b
where b.codigo_linha like s.codigo_linha