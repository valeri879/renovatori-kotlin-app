package ge.renovatori.app.home

/** One carcass package column of the comparison (web `price-list.component.pug` header). */
data class PricePackage(val title: String, val pricePerSquareMeter: Int, val blogUrl: String)

/** One service row: whether it is included in the green / white / black package. */
data class PriceRow(val label: String, val green: Boolean, val white: Boolean, val black: Boolean)

data class PriceGroup(val title: String, val rows: List<PriceRow>)

val pricePackages = listOf(
    PricePackage("მწვანე კარკასი", 550, "https://renovatori.ge/blog/693e40002ed62d127ab05108"),
    PricePackage("თეთრი კარკასი", 600, "https://renovatori.ge/blog/693e40342ed62d127ab0510e"),
    PricePackage("შავი კარკასი", 700, "https://renovatori.ge/blog/693d8d61291bc84d00b6f427")
)

// Generated from views/components/price-list.component.pug; keep in sync with the web table.
val priceGroups = listOf(
    PriceGroup(
        "ელექტრო სამონტაჟო მომსახურება",
        listOf(
            PriceRow("დაკაბელება", false, true, true),
            PriceRow("დენცქვიტები (ყუთები)", true, true, true),
            PriceRow("რევერსული ჩამრთველები (ყუთები)", false, true, true),
            PriceRow("ორ ან მეტ კლავიშიანი შუქის ასანთები (ყუთები)", true, true, true),
            PriceRow("ინტერნეტ კაბელის გაყვანა ოთახებში", false, true, true)
        )
    ),
    PriceGroup(
        "სანტექნიკა / წყალმომარაგება",
        listOf(
            PriceRow("მილსადენების გაყვანა", false, true, true),
            PriceRow("ტრაპის და საკანალიზაციო სისტემის გაკეთება", false, true, true),
            PriceRow("გათბობის ქვაბის მონტაჟი", true, true, true),
            PriceRow("რადიატორების მონტაჟი", true, true, true),
            PriceRow("იატაკის გათბობის მონტაჟი", false, true, true),
            PriceRow("საშრობის მონტაჟი", true, true, true),
            PriceRow("ხელსაბანის მონტაჟი", true, true, true),
            PriceRow("დუშის შემრევი ონკანის მონტაჟი", true, true, true)
        )
    ),
    PriceGroup(
        "იატაკი",
        listOf(
            PriceRow("იატაკის მოხვეწა", false, false, true),
            PriceRow("ლამინირებული იატაკის დაგება", true, true, true),
            PriceRow("კაფელი / მეტლახი / გრანიტის მონტაჟი", true, true, true),
            PriceRow("პლასტმასის / მდფ-ის პლინტუსის მონტაჟი", true, true, true)
        )
    ),
    PriceGroup(
        "კედლები",
        listOf(
            PriceRow("შიდა კედლების (ტიხრების) აშენება", false, false, true),
            PriceRow("ლესვა (გიპერი)", false, false, true),
            PriceRow("ცემენტით ლესვა აბაზანაში", true, true, true),
            PriceRow("ფითხის სამუშაოები", true, true, true),
            PriceRow("ჭერსა და კედელს შორისი ქაფის დაყენება", true, true, true),
            PriceRow("სამღებრო სამუშაოები", true, true, true),
            PriceRow("შპალერის სამუშაოები", true, true, true)
        )
    ),
    PriceGroup(
        "ჭერი",
        listOf(
            PriceRow("თაბაშირ მუყაოს მონტაჟი", true, true, true),
            PriceRow("ფრანგული გასაჭიმი ჭერის მონტაჟი", true, true, true)
        )
    ),
    PriceGroup(
        "კარები",
        listOf(
            PriceRow("MDF კარის მონტაჟი", true, true, true),
            PriceRow("კარის ჩამკეტის მონტაჟი", true, true, true)
        )
    )
)
