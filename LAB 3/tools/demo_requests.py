import argparse, json
from urllib import request

def post(url, data):
    raw=json.dumps(data, ensure_ascii=False).encode('utf-8')
    req=request.Request(url,data=raw,method='POST',headers={'Content-Type':'application/json'})
    with request.urlopen(req,timeout=60) as r:
        return json.loads(r.read().decode('utf-8'))

parser=argparse.ArgumentParser()
parser.add_argument('--host', default='192.168.0.137')
parser.add_argument('--port', default='8080')
args=parser.parse_args()
base=f'http://{args.host}:{args.port}'
items=[
    ('/api/solicitud', {'id_usuario':1,'canal':'WhatsApp','tipo':'solicitud','contenido':'Solicito un prestamo de 5500 soles, tengo sueldo estable'}),
    ('/api/solicitud', {'id_usuario':3,'canal':'SMS','tipo':'solicitud','contenido':'Necesito prestamo urgente de 9000 soles, tengo deuda pendiente y mora'}),
    ('/api/correo', {'id_usuario':5,'asunto':'Refinanciamiento de deuda','tipo':'refinanciamiento','prioridad':'alta','cuerpo':'Tengo atraso de cuotas y deseo refinanciar mi prestamo'})
]
for path, data in items:
    print('\nPOST', path)
    print(json.dumps(post(base+path,data), indent=2, ensure_ascii=False))
