#ifndef MAINWINDOWCLIENTE5__H
#define MAINWINDOWCLIENTE5__H

#include <QMainWindow>

QT_BEGIN_NAMESPACE
namespace Ui {
class MainWindowCliente50;
}
QT_END_NAMESPACE

class MainWindowCliente50 : public QMainWindow
{
    Q_OBJECT

public:
    explicit MainWindowCliente50(QWidget *parent = nullptr);
    ~MainWindowCliente50() override;

private:
    Ui::MainWindowCliente50 *ui;
};
#endif // MAINWINDOWCLIENTE5__H
