#include "mainmenuclient.h"
#include "tcpclient50.h"

#include <QApplication>
#include <QLocale>
#include <QTranslator>
#include <QInputDialog>
#include <QMessageBox>
#include <QLineEdit>


static QString pedirTexto(const QString &mensaje, const QString &defecto)
{
    bool ok = false;
    QString valor = QInputDialog::getText(
        nullptr,
        "MICRO CHUNK - Conexion",
        mensaje,
        QLineEdit::Normal,
        defecto,
        &ok);

    if (!ok || valor.trimmed().isEmpty())
        return defecto;
    return valor.trimmed();
}

static int pedirPuerto(const QString &mensaje, int defecto)
{
    while (true) {
        bool ok = false;
        QString texto = QInputDialog::getText(
            nullptr,
            "MICRO CHUNK - Conexion",
            mensaje,
            QLineEdit::Normal,
            QString::number(defecto),
            &ok);

        if (!ok || texto.trimmed().isEmpty())
            return defecto;

        bool numOk = false;
        int puerto = texto.trimmed().toInt(&numOk);
        if (numOk && puerto >= 1 && puerto <= 65535)
            return puerto;

        QMessageBox::critical(nullptr,
                              "Error de puerto",
                              "Puerto invalido. Ingrese un numero entre 1 y 65535.");
    }
}

// ---------------------------------------------------------------------------
int main(int argc, char *argv[])
{
    QApplication a(argc, argv);

    QTranslator translator;
    const QStringList uiLanguages = QLocale::system().uiLanguages();
    for (const QString &locale : uiLanguages) {
        const QString baseName = "Cliente50_" + QLocale(locale).name();
        if (translator.load(":/i18n/" + baseName)) {
            a.installTranslator(&translator);
            break;
        }
    }

    // Pide datos de conexión
    const QString ip     = pedirTexto("IP del servidor:",      "127.0.0.1");
    const int     port   = pedirPuerto("Puerto del servidor:", TCPClient50::DEFAULT_SERVER_PORT);
    const QString nombre = pedirTexto("Nombre del cliente:",   "Cliente");

    // TCP
    TCPClient50 *tcpClient = new TCPClient50(ip, port, &a);

    QObject::connect(tcpClient, &TCPClient50::connectedToServer,
                     [tcpClient, nombre]() {
                         tcpClient->sendMessage("CLIENT_HELLO|" + nombre);
                     });

    QObject::connect(tcpClient, &TCPClient50::errorOccurred,
                     [](const QString &err) {
                         QMessageBox::critical(nullptr, "Error cliente", err);
                     });

    QObject::connect(tcpClient, &TCPClient50::messageReceived,
                     [](const QString &msg) {
                         qDebug() << "CLIENTE50 recibio:" << msg;
                     });

    tcpClient->connectToServer();

    MainMenuClient *menu = new MainMenuClient(tcpClient, nombre, ip, port);
    menu->show();

    const int result = a.exec();

    tcpClient->stopClient();
    return result;
}
