using System;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

ApplicationConfiguration.Initialize();
Application.Run(new MainForm());

public class MainForm : Form {
    TextBox host = new(){Text="192.168.0.137", Width=140};
    TextBox port = new(){Text="8080", Width=60};
    TextBox id = new(){Text="1", Width=60};
    TextBox contenido = new(){Text="Solicito un prestamo de 5500 soles, tengo sueldo estable", Multiline=true, Width=700, Height=80};
    TextBox output = new(){Multiline=true, ScrollBars=ScrollBars.Vertical, Width=860, Height=320};
    HttpClient http = new();
    public MainForm(){Text="Cliente C# WinForms - Prestamos Distribuidos v8"; Width=920; Height=600; var p=new FlowLayoutPanel(){Dock=DockStyle.Top,Height=160}; p.Controls.Add(new Label(){Text="IP PC1"}); p.Controls.Add(host); p.Controls.Add(new Label(){Text="Puerto"}); p.Controls.Add(port); var b1=new Button(){Text="Probar"}; b1.Click+=async(_,__)=>await Get("/api/health"); p.Controls.Add(b1); p.Controls.Add(new Label(){Text="ID usuario"}); p.Controls.Add(id); p.Controls.Add(contenido); var b2=new Button(){Text="Enviar solicitud",Width=140}; b2.Click+=async(_,__)=>await Post(); p.Controls.Add(b2); Controls.Add(p); output.Dock=DockStyle.Fill; Controls.Add(output);}
    string BaseUrl()=> $"http://{host.Text.Trim()}:{port.Text.Trim()}";
    async Task Get(string path){try{output.Text=await http.GetStringAsync(BaseUrl()+path);}catch(Exception ex){output.Text="ERROR: "+ex.Message;}}
    async Task Post(){try{var json=$"{{\"id_usuario\":{int.Parse(id.Text)},\"canal\":\"WhatsApp\",\"tipo\":\"solicitud\",\"contenido\":\"{contenido.Text.Replace("\\","\\\\").Replace("\"","\\\"").Replace("\r","").Replace("\n"," ")}\"}}"; var res=await http.PostAsync(BaseUrl()+"/api/solicitud",new StringContent(json,Encoding.UTF8,"application/json")); output.Text=await res.Content.ReadAsStringAsync();}catch(Exception ex){output.Text="ERROR: "+ex.Message;}}
}
