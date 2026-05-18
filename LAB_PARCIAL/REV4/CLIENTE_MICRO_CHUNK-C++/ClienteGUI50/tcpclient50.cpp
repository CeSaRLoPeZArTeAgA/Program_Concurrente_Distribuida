#include "tcpclient50.h"

TCPClient50::TCPClient50(const QString &ip, int port, QObject *parent)
    : QObject(parent)
    , m_serverIp(ip)
    , m_serverPort(port)
    , m_socket(new QTcpSocket(this))
{
    connect(m_socket, &QTcpSocket::connected,
            this, &TCPClient50::onConnected);

    connect(m_socket, &QTcpSocket::disconnected,
            this, &TCPClient50::onDisconnected);

    connect(m_socket, &QTcpSocket::readyRead,
            this, &TCPClient50::onReadyRead);

    connect(m_socket, &QAbstractSocket::errorOccurred,
            this, &TCPClient50::onSocketError);
}

TCPClient50::~TCPClient50()
{
    stopClient();
}

void TCPClient50::connectToServer()
{
    if (m_socket->state() == QAbstractSocket::UnconnectedState) {
        m_socket->connectToHost(m_serverIp, static_cast<quint16>(m_serverPort));
    }
}

void TCPClient50::stopClient()
{
    if (m_socket->state() != QAbstractSocket::UnconnectedState) {
        m_socket->disconnectFromHost();
        if (m_socket->state() != QAbstractSocket::UnconnectedState)
            m_socket->waitForDisconnected(2000);
    }
}

void TCPClient50::sendMessage(const QString &message)
{
    if (m_socket->state() == QAbstractSocket::ConnectedState) {
        // El servidor Java usa readLine(), por eso terminamos con '\n'
        m_socket->write((message + "\n").toUtf8());
        m_socket->flush();
    } else {
        emit errorOccurred("No hay conexion activa con el servidor.");
    }
}

bool TCPClient50::isConnected() const
{
    return m_socket->state() == QAbstractSocket::ConnectedState;
}

void TCPClient50::onConnected()
{
    emit connectedToServer();
}

void TCPClient50::onDisconnected()
{
    emit disconnectedFromServer();
}

void TCPClient50::onReadyRead()
{
    while (m_socket->canReadLine()) {
        const QString line = QString::fromUtf8(m_socket->readLine()).trimmed();
        if (!line.isEmpty())
            emit messageReceived(line);
    }
}

void TCPClient50::onSocketError(QAbstractSocket::SocketError /*socketError*/)
{
    emit errorOccurred("Error de red: " + m_socket->errorString());
}
