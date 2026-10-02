package com.paytimeshift.pts.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import java.util.Locale

val LocalLanguage=staticCompositionLocalOf {"mk"}
@Composable fun uiLocale(): Locale = if(LocalLanguage.current=="mk") Locale.forLanguageTag("mk") else Locale.ENGLISH
@Composable fun UiText(text: String,modifier: Modifier=Modifier,color: Color=Color.Unspecified,
    fontSize: TextUnit=14.sp,fontWeight: FontWeight?=null,lineHeight: TextUnit=TextUnit.Unspecified,
    maxLines: Int=Int.MAX_VALUE,overflow: TextOverflow=TextOverflow.Clip,textAlign: TextAlign?=null) {
    androidx.compose.material3.Text(translate(text,LocalLanguage.current),modifier,color=color,fontSize=fontSize,
        fontWeight=fontWeight,lineHeight=if(lineHeight==TextUnit.Unspecified) fontSize*1.25f else lineHeight,maxLines=maxLines,overflow=overflow,textAlign=textAlign)
}
@Composable fun UiText(text: AnnotatedString,modifier: Modifier=Modifier,lineHeight: TextUnit=18.sp) {
    androidx.compose.material3.Text(text,modifier,lineHeight=lineHeight)
}
fun translate(text: String,language: String): String {
    if(language!="mk") return text
    labels[text]?.let {return it}
    var value=text
    fragments.forEach {(en,mk)->value=value.replace(en,mk)}
    return value.replace(Regex("(\\d)h\\b"),"$1 ч")
}
private val labels=mapOf(
    "Shifts and earnings today" to "Смени и заработка денес",
    "Display, reminders and backup" to "Приказ, потсетници и резервна копија",
    "Plan shifts and mark holidays" to "Планирај смени и означи празници",
    "Hours, pay and next payments" to "Часови, заработка и следни исплати",
    "Pay and shift rules" to "Плата и правила за смените",
    "Times, breaks and earnings" to "Часови, паузи и заработка",
    "Repeat and review shifts" to "Повтори и прегледај ги смените",
    "Review before saving" to "Провери пред зачувување",
    "Export as PDF or image" to "Извези како PDF или слика",
    "Choose an earnings period" to "Избери период за заработката",

    "Day of month" to "Ден во месецот",
    "Next / anchor payday" to "Датум на исплата",
    "Import shifts" to "Увези смени",
    "Display" to "Приказ",
    "Currency" to "Валута",
    "Rest between shifts (hours)" to "Одмор меѓу смени (часа)",
    "Rest (hours, e.g. 1.5)" to "Одмор (часа, пр. 1,5)",
    "Reminder (minutes)" to "Потсетник (минути)",
    "Current job prices" to "Актуелни цени",
    "Overtime after (paid hours)" to "Прекувремена по (часа)",
    "Blank = regular price" to "Празно = редовна цена",
    "Holiday: Calendar → date → ⋮" to "Празник: Календар → датум → ⋮",
    "Holidays replace weekend prices. Overtime uses the higher price." to "Празникот има предност пред викендот. За прекувремена важи повисоката цена.",
    "Job & pay" to "Работа и плаќање",
    "Regular hourly rate" to "Редовен час",
    "Hourly prices" to "Цени по час",
    "Overtime after paid hours per shift" to "Прекувремена по платени часа во смена",
    "Overtime hourly rate" to "Прекувремен час",
    "Saturday hourly rate" to "Час во сабота",
    "Sunday hourly rate" to "Час во недела",
    "Holiday hourly rate" to "Час на празник",
    "Daily earnings = paid hours × the applicable hourly price." to "Дневна заработка = платени часови × соодветна цена по час.",
    "A fixed shift amount is paid once per work shift." to "Фиксниот износ се плаќа еднаш за работната смена.",
    "Blank prices use the regular price. Holidays replace weekend prices. Overtime uses the higher applicable price." to "Празно поле = редовна цена. Празникот има предност пред викендот. За прекувремена важи повисоката цена.",
    "Mark holidays in Calendar using the menu for the selected date." to "Празник означуваш во Календар → избери датум → ⋮.",
    "Payments" to "Исплати",
    "Existing shifts" to "Постојни смени",
    "Apply prices to today's and future shifts" to "Примени цени на денешни и идни смени",
    "Past shifts keep their saved prices. Shift times and breaks stay as entered." to "Минатите смени ги задржуваат цените. Часовите и паузите остануваат како што се внесени.",
    "Night additions use the regular hourly price." to "Ноќниот додаток се пресметува врз редовната цена по час.",
    "Holiday" to "Празник",
    "Mark as holiday" to "Означи како празник",
    "Unmark holiday" to "Отстрани ознака за празник",
    "Regular hours" to "Редовни часови",
    "Saturday work" to "Работа во сабота",
    "Holiday work" to "Работа на празник",
    "Use current job prices" to "Користи актуелни цени од работата",
    "Estimated daily earnings" to "Проценета дневна заработка",
    "Hourly prices applied. Unpaid breaks are deducted." to "Цени по час; неплатената пауза е одземена.",
    "Selected color" to "Избрана боја",
    "Calendar actions" to "Опции за календарот",
    "Night starts" to "Ноќна од",
    "Night ends" to "Ноќна до",
    "Use global rest" to "Користи општ одмор", "Use global reminder" to "Користи општ потсетник",
    "Percent additions stack. Overtime is per shift. Untimed unpaid breaks are proportional." to "Додатоците се собираат врз основната цена. Прекувремената е по смена; неплатената пауза се распределува пропорционално.",
    "Days off, vacation and sick leave have no estimated pay." to "Слободните денови, одморот и боледувањето немаат пресметана заработка.",
    "Back" to "Назад", "Night" to "Ноќна", "Afternoon" to "Попладневна", "estimated" to "проценето", "Image" to "Слика", "Android widget" to "Додаток на почетен екран",
    "Backup saved." to "Резервната копија е зачувана.", "File could not be saved." to "Фајлот не се зачува.", "Invalid backup. Current data is unchanged." to "Невалидна копија. Сегашните податоци се задржани.",
    "Could not read file. Try another image or enter text below." to "Фајлот не се прочита. Избери друга слика или внеси текст.",
    "Today" to "Денес", "Calendar" to "Календар", "Earnings" to "Заработка", "Jobs" to "Работи",
    "Job" to "Работа", "Shift" to "Смена", "Your jobs" to "Моите работи", "Estimated earnings" to "Проценета заработка",
    "Separate pay rules for every job." to "Посебни правила за секоја работа.",
    "Add job" to "Додај работа", "Edit job" to "Измени работа", "Save job" to "Зачувај работа", "Job name" to "Име на работата",
    "Add shift" to "Додај смена", "Edit shift" to "Измени смена", "Save shift" to "Зачувај смена", "Delete shift" to "Избриши смена",
    "Cancel" to "Откажи", "Close" to "Затвори", "Delete" to "Избриши", "Save" to "Зачувај", "OK" to "Во ред",
    "Settings" to "Поставки", "Preferences" to "Основни поставки", "Language" to "Јазик",
    "Default currency" to "Почетна валута", "Used for new jobs. Each job keeps its own currency." to "За нови работи. Секоја работа има своја валута.",
    "24-hour time" to "24-часовен приказ", "Start week on Monday" to "Неделата почнува во понеделник", "Appearance" to "Изглед",
    "Light" to "Светол", "Dark" to "Темен", "System" to "Според телефонот",
    "Calendar warnings" to "Предупредувања во календар", "Minimum gap between shifts" to "Минимален одмор меѓу смени",
    "Overlaps and short gaps appear in Calendar." to "Календарот предупредува за преклопување и краток одмор.",
    "Hours (e.g. 1.5)" to "Часови (на пример 1,5)", "Enter 0–168 hours." to "Внеси од 0 до 168 часа.",
    "No shifts added" to "Нема внесени смени", "No shifts added today" to "Нема смени за денес", "Morning" to "Утринска",
    "Next shift" to "Следна смена", "Next payday" to "Следна исплата", "Next payments" to "Следни исплати",
    "This week" to "Оваа недела", "This month" to "Овој месец", "today" to "денес", "planned" to "планирани",
    "estimated earnings" to "проценета заработка", "Based on your pay rules" to "Според твоите правила за плаќање",
    "Week" to "Недела", "Month" to "Месец", "Pay period" to "Платен период", "Choose month" to "Избери месец",
    "Regular base" to "Основна плата", "Fixed shifts" to "По смена", "Total" to "Вкупно",
    "Monthly" to "Месечно", "Weekly" to "Неделно", "Biweekly" to "На две недели", "Custom" to "По избор", "Custom payday" to "Исплата по избор",
    "Fixed amount per shift" to "Фиксен износ по смена", "Amount per shift" to "Износ по смена", "Hourly rate" to "Цена по час",
    "Job color" to "Боја на работата", "Payday frequency" to "Колку често е исплатата", "Existing shifts keep their saved rate and currency." to "Старите смени ја задржуваат својата цена и валута.",
    "Date" to "Датум", "Starts" to "Почеток", "Ends" to "Крај", "Ends the following day" to "Завршува следниот ден",
    "Unpaid break (minutes)" to "Неплатена пауза (минути)", "Break (minutes)" to "Пауза (минути)", "Paid break" to "Платена пауза", "Note (optional)" to "Белешка (по избор)",
    "Delete this shift?" to "Да се избрише смената?", "This removes the shift from your calendar and earnings." to "Смената ќе се отстрани од календарот и заработката.",
    "Enter a job name and a valid rate (0 or more)." to "Внеси име и важечка цена (0 или повеќе).",
    "Start and end must differ." to "Почетокот и крајот мора да се различни.", "Break must be shorter than the shift." to "Паузата мора да биде пократка од смената.", "This shift already exists." to "Оваа смена веќе постои.",
    "Choose currency" to "Избери валута", "Search code or currency name" to "Барај по код или име на валута",
    "One app for every job you work." to "Една апликација за сите твои работи.",
    "Add a job, set your pay and start planning your shifts." to "Додај работа и цена, па испланирај ги смените.",
    "Add your first job" to "Додај ја првата работа", "Explore example schedule" to "Пробај примерен распоред",
    "Load example schedule?" to "Да се додаде примерен распоред?", "Load examples" to "Додај примери",
    "Add Factory, Taxi and Restaurant with example shifts. You can edit them or start with your own jobs instead." to "Ќе се додадат Фабрика, Такси и Ресторан со примерни смени. Можеш да ги измениш.",
    "Shift patterns" to "Повторливи смени", "Create your repeating schedule" to "Направи распоред што се повторува",
    "Buy us a coffee ☕" to "Поддржи нè со кафе ☕", "No ads + automatic backup" to "Без реклами + резервна копија",
    "Explore Premium →" to "Види Premium →", "Local storage · No login required" to "На телефонот · Без најава",
    "Premium & backup" to "Premium и резервна копија", "Free · Local storage" to "Бесплатно · На телефонот",
    "No login required. Cloud backup and purchases are not connected in this preview." to "Без најава. Плаќањето и копијата во облак сè уште не се поврзани.",
    "Coming next" to "Следно", "Ad space" to "Место за реклама", "Saving…" to "Се зачувува…",
    "Changes could not be saved. Please try again." to "Измените не се зачуваа. Обиди се повторно.",
    "Could not read saved data. Your file has been kept. Close the app and contact support before making changes." to "Зачуваните податоци не се прочитаа. Фајлот е зачуван. Затвори ја апликацијата и контактирај поддршка.",
    "Default shift & break" to "Почетна смена и пауза", "Pay additions" to "Додатоци на заработката",
    "Overtime after (hours)" to "Прекувремена по (часови)", "Overtime addition (%)" to "Додаток за прекувремена (%)",
    "Night addition (%)" to "Додаток за ноќна работа (%)", "Sunday addition (%)" to "Додаток за недела (%)", "Night starts" to "Ноќна работа од", "Night ends" to "Ноќна работа до",
    "Bonus amount" to "Бонус (износ)", "Overtime" to "Прекувремена", "Night work" to "Ноќна работа", "Sunday work" to "Работа во недела", "Bonus" to "Бонус",
    "Work" to "Работна смена", "Off" to "Слободен ден", "Vacation" to "Одмор", "Sick" to "Боледување", "Day type" to "Вид на ден",
    "Pay additions included. Break allocation is proportional." to "Додатоци вклучени; паузата е пропорционална.",
    "Reminders" to "Потсетници", "Minutes before shift (0 = off)" to "Минути пред смена (0 = исклучено)",
    "Use global reminder (-1)" to "-1 = според општата поставка", "Job rest (hours, -1 = global)" to "Одмор во часови (-1 = општа поставка)",
    "Archive job" to "Архивирај работа", "Restore job" to "Врати работа", "Archived jobs" to "Архивирани работи",
    "Repeat until" to "Повторувај до", "From" to "Од", "Repeat on days" to "Повторувај во денови", "Preview shifts" to "Преглед на смените", "Add these shifts" to "Додај ги смените",
    "Share schedule" to "Сподели распоред", "Import roster" to "Увези распоред", "Choose image or PDF" to "Избери слика или PDF",
    "Paste or edit recognized text" to "Внеси или поправи го прочитаниот текст", "Review shifts" to "Провери ги смените", "Import confirmed shifts" to "Увези ги проверените смени",
    "Local backup" to "Резервна копија", "Save backup file" to "Зачувај резервна копија", "Restore backup file" to "Врати резервна копија",
    "Restore backup?" to "Да се вратат податоците?", "This replaces your local jobs and shifts. Save a backup first." to "Ќе ги замени сегашните работи и смени. Прво зачувај резервна копија.",
    "Added to calendar." to "Додадено во календарот.", "No matching shifts. Use YYYY-MM-DD HH:mm-HH:mm, one shift per line." to "Нема препознаени смени. Внеси ГГГГ-ММ-ДД ЧЧ:мм-ЧЧ:мм, една смена во ред.",
    "Check dates and times before importing. Nothing is saved automatically." to "Провери ги датумите и часовите. Ништо не се зачувува автоматски.",
    "Image recognition supports Latin text. You can correct or enter Cyrillic text below." to "Читањето слики поддржува латиница. Кирилицата можеш да ја поправиш или внесеш подолу.",
    "Current pay period" to "Тековен платен период", "No active jobs." to "Нема активни работи.",
    "Numbers must be valid and non-negative." to "Внеси важечки ненегативни броеви.", "Select days and valid dates/times; maximum 366 days." to "Избери денови и важечки часови. Најмногу 366 дена.",
    "Premium purchases and cloud backup will be connected after the local core is tested. No payment is taken in this preview." to "Premium плаќањето и автоматската копија во облак сè уште не се поврзани. Нема наплата во оваа верзија.",
    "Add a widget from your phone home screen." to "Додај PTS додаток од почетниот екран на телефонот.",
    "Notifications need permission in phone settings." to "Дозволи известувања во поставките на телефонот.", "Notifications may be delayed by Android battery settings." to "Android може да го одложи потсетникот поради штедење батерија."
)
private val fragments=listOf(
    " paid" to " платени",
    " between shifts" to " одмор меѓу смени", "Overlapping shifts" to "Преклопени смени", " scheduled this month" to " планирани овој месец", " scheduled this week" to " планирани оваа недела", " scheduled in period" to " планирани во периодот",
    "Monthly · Payday " to "Месечно · Исплата на ", "Monthly pay" to "Месечна исплата", "Weekly pay" to "Неделна исплата", "Biweekly pay" to "Исплата на две недели", "Custom pay" to "Исплата по избор",
    "Weekly · " to "Неделно · ", "Biweekly · " to "На две недели · ", " shifts" to " смени", " currencies" to " валути",
    " / year" to " / година", " / hour" to " / час", " / shift" to " / смена", " at " to " во ", "Access until:" to "Пристап до:", "Next billing:" to "Следна наплата:", "Last cloud backup:" to "Последна копија во облак:",
    "Currency · " to "Валута · ", "Day of month · " to "Ден во месецот · ", "Next / anchor payday · " to "Датум на исплата · ",
    "Date · " to "Датум · ", "Starts · " to "Почеток · ", "Ends · " to "Крај · ", "From · " to "Од · ", "Repeat until · " to "До · ", "Night starts · " to "Ноќна од · ", "Night ends · " to "Ноќна до · ",
    "Collapse " to "Собери ", "Expand " to "Прошири ", "0.1.3 · Local core preview" to "0.1.3 · Верзија за тестирање"
)
