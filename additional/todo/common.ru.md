### Список желаемых исправлений

* _готово_ константа Arrays.SUPPORTED_ELEMENT_TYPES
* _готово_ Point/IPoint.distanceFromOrigin: совершенно ЗРЯ используется StrictMath.hypot: я тогда думал, что он быстрый! (больше, слава Богу, нигде его не вызывал)
* _готово_ более краткий toString у типичных Pattern: только засоряет логи
* _готово_ метод Boundary2DSimpleMeasurer.pixelCount
* _готово_ MutableInt128 из stare-java (перенеся в пакет net.algart.math)
* _готово_ метод Matrices asPrecision
* _готово_ неизменяемые массивы вроде EMPTY_DOUBLES, чтобы везде пользоваться
* _сделал вместо этого просто Matrices.clone_ метод Matrix.actualizeSMM (если она не SMM, то клонируется) - им можно воспользоваться в моем MultiMatrix.clone, переименовав его
* _готово (ArraySelector)_ пора наконец добавить класс для относительно эффективного (в среднем линейного) расчета процентили по массиву, вместо сортировки, ибо нужно постоянно
* _готово_ убрать дурацкий ArrayContext у scanBoundary
* _готово_ методы Matrix.maxPossibleValue, bitsPerElement, isFloatingPoint, isUnsigned
* _ничего делать не надо_ исправить https://support.simagis.com/browse/AA-266
* _готово_ (I)RectangularArea: minX/Y/Z, maxX/Y/Z, subtractCollection как метод (вычесть из единственного прямоугольника); valueOf(2 числа) - 1-мерный
* _готово_ (I)RectangularArea: containingRectangle и другие методы из MapRectangularAreas
* _готово_ расчет площадей и периметров в Boundary2DProjectionMeasurer (как выяснилось, многие комбинированные факторы формы требуют и проекций, и площадей с периметрами, так что лучше считать их вместе, синхронно).
* _готово_ Boundary2DScanner - метод reset, сбрасывающий stepCount (и переопределенный в Measurer)
* _не требуется_ Matrices - нужно использовать аннотацию safevararg (не надо, поскольку на входе Matrix<?>..., а не Matrix<T>...)
* JArrays - нужно использовать для минимумов и максимумов встроенный Math.min/max - это в последних JVM гораздо быстрее прямых сравнений

* Надо ***посмотреть*** существующее решение для доступа к большим массивам: https://fastutil.di.unimi.it/ (из https://habr.com/ru/companies/beget/articles/851200/)
* Надо в очередной версии ***отказаться совсем*** от устаревших Arrays.toJavaArray
* в cardinality следует напрямую использовать Long.bitCount - ведь это IntrinsicCandidate! Разница раза в два, в зависимости от измерений. _Cделано_
* Arrays.rangeOf - иправить ошибку с позицией минимума/максимума - она нестабильна в случае многопоточности (при нескольких потоках необходимо сравнивать не только сами значения, но и индексы найденных оптимумов, разумеется, учитывая, что они получены относительно начала каждого диапазона). При этом желательно отказаться от дурацкого приведения целых чисел к типу double!
* _уже сделано_ добавить в Mutable-массивы методы clear()
* _вроде оговорено_ исправить комментарии к перегруженным версиям subMatrix с параметром ContinuationMode: при описании исключений там не оговорен вариант NONE
* ?? кроме Arrays.round32, не повредит функция roundInt, имеющая в качестве нижней границы MIN_VALUE; внутри функции лучше использовать StrictMath. Не повредят также версии этих функций truncate32 / truncateInt, принимающие на вход long, а не double
* возможно, разумнее не раздувать Arrays, а вынести ряд функций в отдельный класс Numbers в пакете math; туда же можно перенести новую функцию equalizeGrid и добавить давно востребованные min,max с 3 и 4 аргументами _не совсем: я не могу по соображениям совместимости вынести в такой класс все функции вроде longMul; что же до equalizeGrid, то я уже прочно забыл ее смысл - значит, она не нужна часто_
* _готово_ добавить в пакет arrays классы наподобие IntArrayAppender, реализующий IntConsumer (IntArrayAppender добавлен локально по месту применения, в универсальном микроклассе такого типа нет смысла)
* _готово (1.4.3)_ методы PArray: toByteArray, toBooleanArray, toIntArray и т.д. (их же продублировать в Matrix) - ими можно будет заменить мой channelToInt/FloatArray
* _готово_ Orthonormal3DBasis,из stare-java-extension (не забыть проапдейтить у них лицензионное соглашение)

* ContourJoiner.interrupter переименовать в ***CancellationChecker***
* ***parallelExecutor*** надо перевести на рельсы fork/join, чтобы он не противоречил распараллеливанию средствами stream - надо убедиться, что если я в параллельном коде со stream вызываю свой parallelExecutor, то дополнительные потоки не создаются; возможно, достаточно использовать newWorkStealingPool
* ***БОЛЬШОЕ УЛУЧШЕНИЕ*** можно попробовать сделать новую версию SimpleMemoryModel-массива для режима unresizable (самого частого), в котором вообще не будет проверок индексов, поскольку все делает сама Java;
* для этого нужно отказаться от никому не нужной логики copy-on-write... впрочем, необязательно: достаточно, если asCopyOnNextWrite будет возвращать массив старого (не столь эффективного) типа
* перенести сюда MatrixBoundariesEmphasizer / SlopeEmphasizer из scichains-cv (net.algart.executors.modules.cv.matrices.misc.slopes)
* Point, IPoint, RectangularArea, IRectangularArea - не хватает методов масштабирования с разными scale по разным координатам
* _готово_ добавить потоково-небезопасные версии BitArray для Simple/BufferMemoryModel, в которых setBit обходился бы без синхронизации - это должно резко ускорить битовое сканирование связных компонент в случае алгоритма без распаковки; можно назвать как-нибудь вроде ThreadLocalBitArray по аналогии с ThreadLocalRandom - _нет, setBitNoSync_
* _готово_ добавить в Arrays возможность эффективно склонировать массив, как Matrices.clone (уже есть updatable/mutableClone, но они не распараллеливают тяжелые объекты, и не так просто выбрать название для метода - clone, по идее, должен быть либо Mutable, либо нет) _метод Arrays.clone_
* перенести MultiMatrix.nonZeroRangeOf в Arrays; отметить полезность его для любого вычисления по маске (ведь можно ненужные элементы обратить в 0)
* оптимизировать rangeOf для битов, если матрица константная: он должен работать с long, а не отдельными битами (если первый long равен 0 или -1, то нужно проверить, не равны ли тому же остальные long)
* JsonTools (? все-таки новая зависимость и версия уже явно не 1.5)
* Matrices.nonZeroPixelsMatrix/zeroPixelsMatrix
* _готово_ Arrays: push/popDouble, Integer для всех MutablePArray/MutablePFixedArray, чтобы не делать занудных веток при добавлении элементов в конец растущего массива (это не так просто, так как требует пересмотра регулярных выражений)
* перенести сюда обработку контуров labelled objects - функционал класса BoundariesScanner - см. https://support.simagis.com/browse/SCVM-372?focusedCommentId=81235&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#action_81235
* _готово_ BGRMatrixToBufferedImageConverter и дурацкий метод SMat.toBitArray
* перенести в net.algart.external QuickBMPReader/Writer из https://github.com/Daniel-Alievsky/pyramid-services/tree/master/pyramid-common/src/main/java/net/algart/imageio
* в перспективе: перенести в AlgART MultiMatrix
* в перспективе: перенести в AlgART SNumbers, скажем, под именем MultiVector
* MutableInt128: нужно **использовать** новую Math.unsignedMultiplyHigh

Надо ***что-то решить*** с удалением временных файлов, которые создаются в MappedDataStorages - не годится, что использование этой технологии необратимо засоряет диск. unsafeUnmap больше не работает (защита на уровне модулей Java 9), нужна альтернатива.

В следующей крупной версии желательно перенести проект на GitHub, сохранив в виде архива проект на BitBucket и поставив там ссылку. 
Заодно можно разобраться, как перенести с сохранением истории. _Сделано_