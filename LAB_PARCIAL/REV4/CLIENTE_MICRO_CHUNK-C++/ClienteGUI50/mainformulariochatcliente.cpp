#include "mainformulariochatcliente.h"
#include "mainmenuclient.h"

#include <QApplication>
#include <QPushButton>
#include <QMessageBox>
#include <QCloseEvent>
#include <QScrollBar>
#include <QDateTime>
#include <QFileDialog>
#include <QFileInfo>
#include <QBuffer>

MainFormularioChatCliente::MainFormularioChatCliente(TCPClient50 *tcpClient,
                                                     MainMenuClient *menu,
                                                     const QString &clientName,
                                                     QWidget *parent)
    : QWidget(parent)
    , ui(new Ui::MainFormularioChatCliente)
    , m_tcpClient(tcpClient)
    , m_menu(menu)
    , m_clientName(clientName)
{
    ui->setupUi(this);

    setWindowTitle(QString("IA Cube - Chat — %1").arg(clientName));
    resize(720, 720);

    if (m_tcpClient->isConnected())
        ui->labelConexion->setText("● Conectado");
    else
        ui->labelConexion->setText("○ Sin conexion");

    ui->labelImagenPreview->setVisible(false);
    ui->labelImagenNombre->setVisible(false);
    ui->labelImagenNombre->setText("");
    ui->btnQuitarImagen->setVisible(false);

    connect(m_tcpClient, &TCPClient50::messageReceived,
            this, &MainFormularioChatCliente::onMessageReceived);
    connect(m_tcpClient, &TCPClient50::connectedToServer,
            this, &MainFormularioChatCliente::onConectado);
    connect(m_tcpClient, &TCPClient50::disconnectedFromServer,
            this, &MainFormularioChatCliente::onDesconectado);

    connect(ui->btnAdjuntar,    &QPushButton::clicked, this, &MainFormularioChatCliente::adjuntarImagen);
    connect(ui->btnEnviar,      &QPushButton::clicked, this, &MainFormularioChatCliente::enviarMensaje);
    connect(ui->btnLimpiar,     &QPushButton::clicked, this, &MainFormularioChatCliente::limpiarChat);
    connect(ui->btnMenu,        &QPushButton::clicked, this, &MainFormularioChatCliente::close);
    connect(ui->btnSalir,       &QPushButton::clicked, this, &MainFormularioChatCliente::salirDelSistema);
    connect(ui->btnQuitarImagen,&QPushButton::clicked, this, [this]() {
        m_pendingImage     = QImage();
        m_pendingImageName.clear();
        ui->labelImagenPreview->setVisible(false);
        ui->labelImagenNombre->setVisible(false);
        ui->btnQuitarImagen->setVisible(false);
    });

    connect(ui->inputMensaje, &QLineEdit::returnPressed,
            this, &MainFormularioChatCliente::enviarMensaje);
}

MainFormularioChatCliente::~MainFormularioChatCliente()
{
    delete ui;
}

void MainFormularioChatCliente::closeEvent(QCloseEvent *event)
{
    disconnect(m_tcpClient, &TCPClient50::messageReceived,
               this, &MainFormularioChatCliente::onMessageReceived);
    disconnect(m_tcpClient, &TCPClient50::connectedToServer,
               this, &MainFormularioChatCliente::onConectado);
    disconnect(m_tcpClient, &TCPClient50::disconnectedFromServer,
               this, &MainFormularioChatCliente::onDesconectado);
    m_menu->volverAMostrar();
    event->accept();
}

void MainFormularioChatCliente::adjuntarImagen()
{
    const QString filePath = QFileDialog::getOpenFileName(
        this,
        "Adjuntar imagen",
        QString(),
        "Imagenes (*.png *.jpg *.jpeg)");

    if (filePath.isEmpty()) return;

    QImage img(filePath);
    if (img.isNull()) {
        QMessageBox::critical(this, "Error",
                              "El archivo seleccionado no es una imagen valida.");
        return;
    }

    m_pendingImage     = img;
    m_pendingImageName = QFileInfo(filePath).fileName();

    // Mostrar miniatura en el área de preview
    const QPixmap px = QPixmap::fromImage(img).scaled(64, 64,
                                                      Qt::KeepAspectRatio,
                                                      Qt::SmoothTransformation);
    ui->labelImagenPreview->setPixmap(px);
    ui->labelImagenPreview->setVisible(true);
    ui->labelImagenNombre->setText(m_pendingImageName);
    ui->labelImagenNombre->setVisible(true);
    ui->btnQuitarImagen->setVisible(true);
}

void MainFormularioChatCliente::enviarMensaje()
{
    if (!m_tcpClient->isConnected()) {
        QMessageBox::critical(this, "Error",
                              "No hay conexion activa con el servidor.");
        return;
    }

    // CASO A: hay imagen adjunta
    if (!m_pendingImage.isNull()) {
        appendMensaje(m_clientName,
                      QString("[Imagen adjunta: %1]").arg(m_pendingImageName),
                      true);
        appendImagenPreview(m_clientName,
                            QPixmap::fromImage(m_pendingImage).scaled(
                                120, 120, Qt::KeepAspectRatio, Qt::SmoothTransformation),
                            true);
        enviarComoMNIST(m_pendingImage);

        // Limpiar adjunto
        m_pendingImage     = QImage();
        m_pendingImageName.clear();
        ui->labelImagenPreview->setVisible(false);
        ui->labelImagenNombre->setVisible(false);
        ui->btnQuitarImagen->setVisible(false);

        // si ademas hay texto, enviarlo como Word2Vec después
        const QString texto = ui->inputMensaje->text().trimmed();
        if (!texto.isEmpty()) {
            appendMensaje(m_clientName, texto, true);
            enviarComoW2V(texto);
            ui->inputMensaje->clear();
        }
        return;
    }

    //CASO B: solo texto
    const QString texto = ui->inputMensaje->text().trimmed();
    if (texto.isEmpty()) return;

    appendMensaje(m_clientName, texto, true);
    enviarComoW2V(texto);
    ui->inputMensaje->clear();
}


void MainFormularioChatCliente::enviarComoMNIST(const QImage &img)
{
    const QImage gray =
        img.scaled(28, 28, Qt::IgnoreAspectRatio, Qt::FastTransformation)
            .convertToFormat(QImage::Format_Grayscale8);

    QStringList parts;
    parts.reserve(784);
    for (int y = 0; y < 28; y++) {
        const uchar *line = gray.constScanLine(y);
        for (int x = 0; x < 28; x++)
            parts << QString::number(line[x] / 255.0, 'f', 8);
    }

    appendMensaje("Sistema", "Enviando imagen al servidor IA (MNIST)...", false);
    m_tcpClient->sendMessage("MNIST_PREDICT|" + parts.join(','));
}


void MainFormularioChatCliente::enviarComoW2V(const QString &texto)
{
    auto safe = [](const QString &t) {
        QString s = t;
        s.replace('|',  '/');
        s.replace('\n', ' ');
        s.replace('\r', ' ');
        return s.trimmed();
    };

    const QStringList tokens = texto.split(':', Qt::SkipEmptyParts);
    if (tokens.size() == 3) {
        const QString a = safe(tokens[0]);
        const QString b = safe(tokens[1]);
        const QString c = safe(tokens[2]);

        if (!a.isEmpty() && !b.isEmpty() && !c.isEmpty()) {
            appendMensaje("Sistema",
                          QString("Calculando analogia: %1:%2:%3 → ? (Word2Vec)").arg(a).arg(b).arg(c),
                          false);
            // Protocolo: "W2V_ANALOGY|A|B|C|topN"
            m_tcpClient->sendMessage("W2V_ANALOGY|" + a + "|" + b + "|" + c + "|10");
            return;
        }
    }

    //Caso general: búsqueda de palabras cercanas
    const QString s = safe(texto);
    appendMensaje("Sistema", "Buscando palabras cercanas (Word2Vec)...", false);
    m_tcpClient->sendMessage("W2V_NEAREST|" + s + "|10");
}

void MainFormularioChatCliente::limpiarChat()
{
    ui->chatHistorial->clear();
}

void MainFormularioChatCliente::onMessageReceived(const QString &message)
{
    if (message.startsWith("MNIST_RESULT|")) {
        // "MNIST_RESULT|<prediccion>|<prob0,prob1,...>|<tiempoMs>"
        const QStringList parts = message.split('|');
        if (parts.size() < 4) return;

        const int prediccion = parts[1].trimmed().toInt();
        const QStringList probs = parts[2].split(',');
        const QString tiempoMs  = parts[3].trimmed();

        QString sb;
        sb += QString("Prediccion del servidor IA: %1\n\n").arg(prediccion);
        sb += "Probabilidades por clase:\n";
        for (int i = 0; i < probs.size(); i++)
            sb += QString("  Clase %1 : %2\n").arg(i).arg(probs[i].trimmed());
        sb += QString("\nTiempo en servidor: %1 ms").arg(tiempoMs);

        appendMensaje("Servidor IA (MNIST)", sb, false);
        return;
    }

    // Respuesta Word2Vec
    if (message.startsWith("W2V_NEAREST_RESULT|")) {
        const QStringList parts = message.split('|');
        if (parts.size() < 4) return;

        const QString consulta = parts[1];
        const QString ranking  = parts[2];
        const QString tiempoMs = parts[3];

        QString sb;
        sb += QString("Palabras mas cercanas a \"%1\":\n\n").arg(consulta);
        if (ranking.trimmed().isEmpty()) {
            sb += "No se encontraron resultados.";
        } else {
            const QStringList items = ranking.split(',');
            for (int i = 0; i < items.size(); i++) {
                const QStringList kv = items[i].split(':');
                const QString palabra = kv.size() >= 1 ? kv[0] : "";
                const QString sim     = kv.size() >= 2 ? kv[1] : "";
                sb += QString("%1. %2 sim=%3\n")
                          .arg(i + 1, 2)
                          .arg(palabra.leftJustified(20))
                          .arg(sim);
            }
        }
        sb += QString("\nTiempo en servidor: %1 ms").arg(tiempoMs);
        appendMensaje("Servidor IA (Word2Vec)", sb, false);
        return;
    }

    //Respuesta Word2Vec (analogia)
    if (message.startsWith("W2V_RESULT|")) {
        const QStringList parts = message.split('|');
        if (parts.size() < 4) return;

        const QString consulta = parts[1];
        const QString ranking  = parts[2];
        const QString tiempoMs = parts[3];

        QString sb;
        sb += QString("Analogia \"%1\":\n\n").arg(consulta);
        const QStringList items = ranking.split(',');
        for (int i = 0; i < items.size(); i++) {
            const QStringList kv = items[i].split(':');
            const QString palabra = kv.size() >= 1 ? kv[0] : "";
            const QString sim     = kv.size() >= 2 ? kv[1] : "";
            sb += QString("%1. %2 sim=%3\n")
                      .arg(i + 1, 2)
                      .arg(palabra.leftJustified(20))
                      .arg(sim);
        }
        sb += QString("\nTiempo en servidor: %1 ms").arg(tiempoMs);
        appendMensaje("Servidor IA (Word2Vec)", sb, false);
        return;
    }

    //Chat normal
    if (message.startsWith("CHAT_MSG|")) {
        const QStringList parts = message.split('|');
        if (parts.size() < 3) return;
        appendMensaje(parts[1].trimmed(), parts[2].trimmed(), false);
        return;
    }

    // Error
    if (message.startsWith("ERROR|")) {
        const QStringList parts = message.split('|');
        const QString error = parts.size() >= 2 ? parts[1] : "Error desconocido";
        appendMensaje("Sistema", "Error: " + error, false);
    }
}

void MainFormularioChatCliente::onConectado()
{
    ui->labelConexion->setText("● Conectado");
}

void MainFormularioChatCliente::onDesconectado()
{
    ui->labelConexion->setText("○ Sin conexion");
}

void MainFormularioChatCliente::appendMensaje(const QString &id,
                                              const QString &texto,
                                              bool esPropio)
{
    const QString colorId    = esPropio ? "#1B3A6B" : "#8A97AA";
    const QString alineacion = esPropio ? "right"   : "left";
    const QString hora       = QDateTime::currentDateTime().toString("hh:mm");

    const QString textoHtml = texto.toHtmlEscaped().replace("\n", "<br>");

    const QString html =
        QString("<div style='text-align:%1; margin-bottom:12px;'>"
                "<span style='font-size:8pt; color:%2; font-family:Arial;'>%3 &nbsp; %4</span><br>"
                "<span style='font-size:10pt; color:#111111; font-family:Arial;'>%5</span>"
                "</div>")
            .arg(alineacion)
            .arg(colorId)
            .arg(id.toHtmlEscaped())
            .arg(hora)
            .arg(textoHtml);

    ui->chatHistorial->append(html);
    ui->chatHistorial->verticalScrollBar()->setValue(
        ui->chatHistorial->verticalScrollBar()->maximum());
}

void MainFormularioChatCliente::appendImagenPreview(const QString &id,
                                                    const QPixmap &px,
                                                    bool esPropio)
{
    // Inserta imagen inline en el QTextEdit usando base64
    QByteArray ba;
    QBuffer buf(&ba);
    buf.open(QIODevice::WriteOnly);
    px.toImage().save(&buf, "PNG");
    const QString b64 = QString::fromLatin1(ba.toBase64());

    const QString alineacion = esPropio ? "right" : "left";
    const QString html =
        QString("<div style='text-align:%1; margin-bottom:4px;'>"
                "<img src='data:image/png;base64,%2' style='border:1px solid #ccc;'/>"
                "</div>")
            .arg(alineacion)
            .arg(b64);

    ui->chatHistorial->append(html);
    ui->chatHistorial->verticalScrollBar()->setValue(
        ui->chatHistorial->verticalScrollBar()->maximum());
}

void MainFormularioChatCliente::salirDelSistema()
{
    const auto resp = QMessageBox::question(
        this, "Confirmar salida",
        "Deseas cerrar el cliente?",
        QMessageBox::Yes | QMessageBox::No);

    if (resp == QMessageBox::Yes) {
        m_tcpClient->stopClient();
        QApplication::quit();
    }
}