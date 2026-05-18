#include "mainwindowcliente50.h"
#include "./ui_mainwindowcliente50.h"

MainWindowCliente50::MainWindowCliente50(QWidget *parent)
    : QMainWindow(parent)
    , ui(new Ui::MainWindowCliente50)
{
    ui->setupUi(this);
}

MainWindowCliente50::~MainWindowCliente50()
{
    delete ui;
}
