import datetime
import http.client
import json
import psycopg2

def get_value_or_default(arr, key, default = 0):
    val = ''
    try:
        val = arr[key]
    except:
        val = default
    return val

conn = http.client.HTTPSConnection("temporeal.pbh.gov.br")
payload = ''
headers = {'User-Agent': 'Mozilla/5.0 (X11; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/115.0'}
conn.request("GET", "/?param=D", payload, headers)
res = conn.getresponse()
data = res.read()
decoded = json.loads(data.decode("utf-8"))

print(f"{len(decoded)} novos registros em {datetime.datetime.today()}")
con = psycopg2.connect(host='localhost', port=15432, database='bh',
            user='bh', password='bh')
cursor = con.cursor()
sql = """
    INSERT INTO bh.real_time_bus
(dt_entry, coord, id_vehicle, id_line, current_distance_traveled)
VALUES( TO_TIMESTAMP(%s, 'YYYYMMDDHH24MISS'), ST_MakePoint(%s, %s), %s, %s, %s);
"""

for p in decoded:
    if 'NL' in p:
        try:
            cursor.execute(sql, (p['HR'], p['LG'], p['LT'], p['NV'], p['NL'], get_value_or_default(p, 'DT'), ))
        except:
            print(f"registro sem linha correspondente {p}")
    else:
        print(f"registro sem linha de onibus {p}")

con.commit()
cursor.close()
