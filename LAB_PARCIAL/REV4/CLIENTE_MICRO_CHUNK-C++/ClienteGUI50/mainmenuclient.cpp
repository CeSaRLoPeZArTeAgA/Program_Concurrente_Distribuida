#include "mainmenuclient.h"
#include "mainformulariochatcliente.h"

#include <QApplication>
#include <QScreen>
#include <QMessageBox>

MainMenuClient::MainMenuClient(TCPClient50 *tcpClient,
                               const QString &clientName,
                               const QString &serverIp,
                               int serverPort,
                               QWidget *parent)
    : QWidget(parent)
    , ui(new Ui::MainMenuClient)
    , m_tcpClient(tcpClient)
    , m_clientName(clientName)
    , m_serverIp(serverIp)
    , m_serverPort(serverPort)
{
    ui->setupUi(this);

    setWindowTitle(QString("Cliente MICRO CHUNK - %1 - %2:%3")
                       .arg(clientName).arg(serverIp).arg(serverPort));
    setFixedSize(380, 560);
    setAttribute(Qt::WA_QuitOnClose, true);

    ui->labelEstadoServidor->setText(
        QString("Servidor: %1:%2").arg(serverIp).arg(serverPort));

    connect(ui->btnEntrar, &QPushButton::clicked, this, &MainMenuClient::entrarAlModelo);
    connect(ui->btnSalir,  &QPushButton::clicked, this, &MainMenuClient::salirDelSistema);
}

MainMenuClient::~MainMenuClient()
{
    delete ui;
}

void MainMenuClient::entrarAlModelo()
{
    if (!m_tcpClient->isConnected()) {
        QMessageBox::warning(this,
                             "Conexion no lista",
                             "Todavia no hay conexion activa con el servidor.");
        return;
    }

    MainFormularioChatCliente *f =
        new MainFormularioChatCliente(m_tcpClient, this, m_clientName);
    f->setAttribute(Qt::WA_DeleteOnClose);
    f->show();
    hide();
}

void MainMenuClient::volverAMostrar()
{
    if (const QScreen *sc = QApplication::primaryScreen()) {
        const QRect sg = sc->geometry();
        move(sg.center() - rect().center());
    }
    show();
    raise();
    activateWindow();
}

void MainMenuClient::salirDelSistema()
{
    const auto resp = QMessageBox::question(
        this, "Confirmar salida",
        "Deseas salir del sistema?",
        QMessageBox::Yes | QMessageBox::No);

    if (resp == QMessageBox::Yes) {
        m_tcpClient->stopClient();
        QApplication::quit();
    }
}
