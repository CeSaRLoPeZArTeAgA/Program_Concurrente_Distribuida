#ifndef MAINMENUCLIENT_H
#define MAINMENUCLIENT_H

#include <QWidget>

#include "tcpclient50.h"
#include "ui_mainmenuclient.h"

class MainMenuClient : public QWidget
{
    Q_OBJECT

public:
    explicit MainMenuClient(TCPClient50 *tcpClient,
                            const QString &clientName,
                            const QString &serverIp,
                            int serverPort,
                            QWidget *parent = nullptr);
    ~MainMenuClient() override;

    void volverAMostrar();

private slots:
    void entrarAlModelo();
    void salirDelSistema();

private:
    Ui::MainMenuClient *ui;

    TCPClient50 *m_tcpClient;
    QString      m_clientName;
    QString      m_serverIp;
    int          m_serverPort;
};

#endif // MAINMENUCLIENT_H
