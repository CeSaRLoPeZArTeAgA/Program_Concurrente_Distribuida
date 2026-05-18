#ifndef TCPCLIENT50_H
#define TCPCLIENT50_H

#include <QObject>
#include <QTcpSocket>
#include <QString>

class TCPClient50 : public QObject
{
    Q_OBJECT

public:
    static constexpr int DEFAULT_SERVER_PORT = 4444;

    explicit TCPClient50(const QString &ip,
                         int port      = DEFAULT_SERVER_PORT,
                         QObject *parent = nullptr);
    ~TCPClient50() override;

    void connectToServer();
    void stopClient();
    void sendMessage(const QString &message);
    bool isConnected() const;

    QString serverIp()   const { return m_serverIp;   }
    int     serverPort() const { return m_serverPort;  }

signals:
    void messageReceived(const QString &message);
    void connectedToServer();
    void disconnectedFromServer();
    void errorOccurred(const QString &errorMessage);

private slots:
    void onConnected();
    void onDisconnected();
    void onReadyRead();
    void onSocketError(QAbstractSocket::SocketError socketError);

private:
    QString     m_serverIp;
    int         m_serverPort;
    QTcpSocket *m_socket;
};

#endif // TCPCLIENT50_H
