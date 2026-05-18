# ClienteGUI
### Ejecucion:
> Es necesario Tener instalado QT

##### build
```
cd ClienteGUI50
mkdir build
cd build

& "C:\Qt\Tools\CMake_64\bin\cmake.exe" .. `
    -G "MinGW Makefiles" `
    -DCMAKE_PREFIX_PATH="C:/Qt/6.11.0/mingw_64" `
    -DCMAKE_BUILD_TYPE=Release `
    -DCMAKE_MAKE_PROGRAM="C:/Qt/Tools/mingw1310_64/bin/mingw32-make.exe"

& "C:\Qt\Tools\CMake_64\bin\cmake.exe" --build . --parallel

& "C:\Qt\6.11.0\mingw_64\bin\windeployqt.exe" --release --no-translations .\Cliente50.exe

```
##### Run
```
.\Cliente50.exe
```
