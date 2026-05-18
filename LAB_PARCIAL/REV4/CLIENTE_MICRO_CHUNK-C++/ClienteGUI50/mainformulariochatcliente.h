#ifndef MAINFORMULARIOCHATCLIENTE_H
#define MAINFORMULARIOCHATCLIENTE_H

#include <QWidget>
#include <QImage>

#include "tcpclient50.h"
#include "ui_mainformulariochatcliente.h"

class MainMenuClient;

class MainFormularioChatCliente : public QWidget
{
    Q_OBJECT

public:
    explicit MainFormularioChatCliente(TCPClient50 *tcpClient,
                                       MainMenuClient *menu,
                                       const QString &clientName,
                                       QWidget *parent = nullptr);
    ~MainFormularioChatCliente() override;

protected:
    void closeEvent(QCloseEvent *event) override;

private slots:
    void enviarMensaje();
    void adjuntarImagen();
    void limpiarChat();
    void onMessageReceived(const QString &message);
    void onConectado();
    void onDesconectado();
    void salirDelSistema();

private:
    void appendMensaje(const QString &id, const QString &texto, bool esPropio);
    void appendImagenPreview(const QString &id, const QPixmap &px, bool esPropio);
    void enviarComoMNIST(const QImage &img);
    void enviarComoW2V(const QString &texto);

    Ui::MainFormularioChatCliente *ui;

    TCPClient50    *m_tcpClient;
    MainMenuClient *m_menu;
    QString         m_clientName;

    QImage  m_pendingImage;
    QString m_pendingImageName;
};

#endif // MAINFORMULARIOCHATCLIENTE_H
