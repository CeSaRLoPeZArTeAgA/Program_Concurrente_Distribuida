import argparse, concurrent.futures, json, random, time
from urllib import request

def post(url, data):
    raw=json.dumps(data, ensure_ascii=False).encode('utf-8')
    req=request.Request(url,data=raw,method='POST',headers={'Content-Type':'application/json'})
    with request.urlopen(req,timeout=90) as r:
        return r.status, r.read().decode('utf-8')[:200]

def main():
    ap=argparse.ArgumentParser(); ap.add_argument('--host',default='192.168.0.137'); ap.add_argument('--port',default='8080'); ap.add_argument('--n',type=int,default=1000); ap.add_argument('--workers',type=int,default=20); args=ap.parse_args()
    base=f'http://{args.host}:{args.port}'
    textos=['Solicito un prestamo de 2500 soles, tengo sueldo estable','Necesito prestamo urgente de 9000 soles, tengo deuda pendiente','Quiero refinanciar mi prestamo por atraso de cuotas','Consulta de prestamo activo']
    def task(i):
        data={'id_usuario':random.randint(1,6),'canal':'WhatsApp','tipo':'solicitud','contenido':random.choice(textos)}
        return post(base+'/api/solicitud', data)[0]
    t0=time.time(); ok=0; err=0
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.workers) as ex:
        for r in concurrent.futures.as_completed([ex.submit(task,i) for i in range(args.n)]):
            try: ok += 1 if r.result()==200 else 0
            except Exception: err += 1
    dt=time.time()-t0
    print(json.dumps({'n':args.n,'ok':ok,'errors':err,'seconds':dt,'req_per_sec':args.n/dt}, indent=2))
if __name__=='__main__': main()
