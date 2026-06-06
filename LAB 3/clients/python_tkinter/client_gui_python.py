import json
import tkinter as tk
from tkinter import ttk, messagebox
from urllib import request, error


def post_json(url, data, timeout=45):
    raw = json.dumps(data, ensure_ascii=False).encode('utf-8')
    req = request.Request(url, data=raw, method='POST', headers={'Content-Type': 'application/json'})
    with request.urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode('utf-8'))


def get_json(url, timeout=10):
    with request.urlopen(url, timeout=timeout) as resp:
        return json.loads(resp.read().decode('utf-8'))


class App(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title('Cliente Python Tkinter - Prestamos Distribuidos v8')
        self.geometry('980x620')
        self.host = tk.StringVar(value='192.168.0.137')
        self.port = tk.StringVar(value='8080')
        self._build()

    def base(self):
        return f'http://{self.host.get().strip()}:{self.port.get().strip()}'

    def _build(self):
        top = ttk.LabelFrame(self, text='Conexion a PC1 - API de negocio')
        top.pack(fill='x', padx=10, pady=8)
        ttk.Label(top, text='IP PC1').pack(side='left', padx=5)
        ttk.Entry(top, textvariable=self.host, width=18).pack(side='left')
        ttk.Label(top, text='Puerto').pack(side='left', padx=5)
        ttk.Entry(top, textvariable=self.port, width=8).pack(side='left')
        ttk.Button(top, text='Probar conexion', command=self.test).pack(side='left', padx=10)
        self.nb = ttk.Notebook(self); self.nb.pack(fill='both', expand=True, padx=10, pady=4)
        self.tab_sms = ttk.Frame(self.nb); self.tab_correo = ttk.Frame(self.nb); self.tab_consulta = ttk.Frame(self.nb)
        self.nb.add(self.tab_sms, text='SMS / WhatsApp')
        self.nb.add(self.tab_correo, text='Correo')
        self.nb.add(self.tab_consulta, text='Consulta')
        self._sms(); self._correo(); self._consulta()
        self.out = tk.Text(self, height=12, wrap='word')
        self.out.pack(fill='both', expand=True, padx=10, pady=8)

    def _sms(self):
        self.sms_id = tk.StringVar(value='1'); self.sms_canal = tk.StringVar(value='WhatsApp'); self.sms_tipo = tk.StringVar(value='solicitud')
        ttk.Label(self.tab_sms, text='ID usuario').pack(anchor='w', padx=10, pady=3)
        ttk.Entry(self.tab_sms, textvariable=self.sms_id).pack(fill='x', padx=10)
        ttk.Label(self.tab_sms, text='Canal').pack(anchor='w', padx=10, pady=3)
        ttk.Entry(self.tab_sms, textvariable=self.sms_canal).pack(fill='x', padx=10)
        ttk.Label(self.tab_sms, text='Tipo').pack(anchor='w', padx=10, pady=3)
        ttk.Entry(self.tab_sms, textvariable=self.sms_tipo).pack(fill='x', padx=10)
        ttk.Label(self.tab_sms, text='Contenido').pack(anchor='w', padx=10, pady=3)
        self.sms_contenido = tk.Text(self.tab_sms, height=7)
        self.sms_contenido.pack(fill='both', expand=True, padx=10)
        self.sms_contenido.insert('1.0', 'Solicito un prestamo de 5500 soles, tengo sueldo estable')
        ttk.Button(self.tab_sms, text='Solicitar prestamo por SMS/WhatsApp', command=self.send_sms).pack(pady=8)

    def _correo(self):
        self.correo_id = tk.StringVar(value='2'); self.correo_tipo = tk.StringVar(value='refinanciamiento'); self.correo_prio = tk.StringVar(value='alta')
        self.correo_asunto = tk.StringVar(value='Solicitud de refinanciamiento')
        for txt, var in [('ID usuario', self.correo_id), ('Asunto', self.correo_asunto), ('Tipo', self.correo_tipo), ('Prioridad', self.correo_prio)]:
            ttk.Label(self.tab_correo, text=txt).pack(anchor='w', padx=10, pady=3)
            ttk.Entry(self.tab_correo, textvariable=var).pack(fill='x', padx=10)
        ttk.Label(self.tab_correo, text='Cuerpo').pack(anchor='w', padx=10, pady=3)
        self.correo_cuerpo = tk.Text(self.tab_correo, height=7)
        self.correo_cuerpo.pack(fill='both', expand=True, padx=10)
        self.correo_cuerpo.insert('1.0', 'Quiero refinanciar mi prestamo porque tengo atraso de cuotas')
        ttk.Button(self.tab_correo, text='Enviar correo', command=self.send_correo).pack(pady=8)

    def _consulta(self):
        self.req_id = tk.StringVar()
        ttk.Button(self.tab_consulta, text='Health PC1', command=self.test).pack(pady=8)
        ttk.Button(self.tab_consulta, text='Metricas PC1', command=self.metrics).pack(pady=8)
        ttk.Button(self.tab_consulta, text='Ultimas solicitudes', command=self.requests).pack(pady=8)
        ttk.Label(self.tab_consulta, text='Request ID').pack(anchor='w', padx=10)
        ttk.Entry(self.tab_consulta, textvariable=self.req_id).pack(fill='x', padx=10)
        ttk.Button(self.tab_consulta, text='Consultar request_id', command=self.get_request).pack(pady=8)

    def show(self, obj):
        self.out.delete('1.0', 'end')
        self.out.insert('end', json.dumps(obj, indent=2, ensure_ascii=False))

    def err(self, exc):
        self.out.delete('1.0', 'end')
        self.out.insert('end', 'ERROR: ' + str(exc))

    def test(self):
        try: self.show(get_json(self.base() + '/api/health'))
        except Exception as e: self.err(e)

    def metrics(self):
        try: self.show(get_json(self.base() + '/api/metrics'))
        except Exception as e: self.err(e)

    def requests(self):
        try: self.show(get_json(self.base() + '/api/requests'))
        except Exception as e: self.err(e)

    def get_request(self):
        try: self.show(get_json(self.base() + '/api/requests/' + self.req_id.get().strip()))
        except Exception as e: self.err(e)

    def send_sms(self):
        try:
            data = {'id_usuario': int(self.sms_id.get()), 'canal': self.sms_canal.get(), 'tipo': self.sms_tipo.get(), 'contenido': self.sms_contenido.get('1.0', 'end').strip()}
            self.show(post_json(self.base() + '/api/solicitud', data))
        except Exception as e: self.err(e)

    def send_correo(self):
        try:
            data = {'id_usuario': int(self.correo_id.get()), 'asunto': self.correo_asunto.get(), 'tipo': self.correo_tipo.get(), 'prioridad': self.correo_prio.get(), 'cuerpo': self.correo_cuerpo.get('1.0', 'end').strip()}
            self.show(post_json(self.base() + '/api/correo', data))
        except Exception as e: self.err(e)

if __name__ == '__main__':
    App().mainloop()
