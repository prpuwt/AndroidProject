import os
import io
import docx
from docx import Document
from docx.shared import Pt, Inches
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
from PIL import Image, ImageDraw, ImageFont

doc = Document()

# ============ Helper Functions ============

def set_run_font(run, font_name, size_pt, bold=False):
    run.font.size = Pt(size_pt)
    run.bold = bold
    run.font.name = font_name
    r = run._element
    rPr = r.find(qn('w:rPr'))
    if rPr is None:
        rPr = docx.oxml.OxmlElement('w:rPr')
        r.insert(0, rPr)
    rFonts = rPr.find(qn('w:rFonts'))
    if rFonts is None:
        rFonts = docx.oxml.OxmlElement('w:rFonts')
        rPr.append(rFonts)
    rFonts.set(qn('w:eastAsia'), font_name)


def set_paragraph_spacing(p, before=0, after=0):
    pf = p.paragraph_format
    pf.space_before = Pt(before)
    pf.space_after = Pt(after)


def add_title(text):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run(text)
    set_run_font(run, 'SimHei', 22, bold=True)
    set_paragraph_spacing(p, before=0, after=6)
    return p


def add_heading_large(text):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run(text)
    set_run_font(run, 'SimHei', 18, bold=True)
    set_paragraph_spacing(p, before=6, after=6)
    return p


def add_chapter_title(text):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run(text)
    set_run_font(run, 'SimHei', 18, bold=True)
    set_paragraph_spacing(p, before=6, after=6)
    return p


def add_section_title(text):
    p = doc.add_paragraph()
    run = p.add_run(text)
    set_run_font(run, 'SimHei', 15, bold=True)
    set_paragraph_spacing(p, before=6, after=6)
    return p


def add_subsection_title(text):
    p = doc.add_paragraph()
    run = p.add_run(text)
    set_run_font(run, 'SimHei', 12, bold=True)
    set_paragraph_spacing(p, before=6, after=6)
    return p


def add_body(text):
    p = doc.add_paragraph()
    p.paragraph_format.first_line_indent = Pt(24)
    run = p.add_run(text)
    set_run_font(run, 'SimSun', 12)
    set_paragraph_spacing(p, before=0, after=3)
    return p


def add_reference(text):
    p = doc.add_paragraph()
    run = p.add_run(text)
    set_run_font(run, 'SimSun', 10.5)
    set_paragraph_spacing(p, before=0, after=2)
    return p


def add_keywords(text):
    p = doc.add_paragraph()
    run = p.add_run(text)
    set_run_font(run, 'SimHei', 12, bold=True)
    set_paragraph_spacing(p, before=6, after=6)
    return p


# ============ Code Screenshot Generator ============

def generate_code_screenshot(code_text, title="", width=800, line_height=28):
    font_size = 12
    try:
        font = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", font_size)
        title_font = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 14)
    except:
        font = ImageFont.load_default()
        title_font = font

    lines = code_text.split('\n')
    title_height = 36 if title else 0
    padding = 20
    content_height = len(lines) * line_height + padding * 2
    total_height = title_height + content_height

    img = Image.new('RGB', (width, total_height), (30, 30, 30))
    draw = ImageDraw.Draw(img)

    if title:
        draw.rectangle([0, 0, width, title_height], fill=(45, 45, 45))
        draw.text((14, 8), title, font=title_font, fill=(200, 200, 200))
        y_offset = title_height
    else:
        y_offset = padding

    y = y_offset + padding
    for line in lines:
        col = (255, 255, 255)
        stripped = line.strip()
        if stripped.startswith('//') or stripped.startswith('#'):
            col = (150, 200, 100)
        elif stripped.startswith('import') or stripped.startswith('package'):
            col = (200, 150, 255)
        elif stripped.startswith(('public ', 'private ', 'protected ')):
            col = (100, 200, 255)
        elif stripped.startswith(('class ', 'interface ')):
            col = (255, 220, 100)
        elif stripped.startswith('@'):
            col = (180, 180, 180)
        draw.text((20, y), line, font=font, fill=col)
        y += line_height

    return img


def add_code_screenshot(code_text, title="", caption=""):
    img = generate_code_screenshot(code_text, title)
    buf = io.BytesIO()
    img.save(buf, format='PNG')
    buf.seek(0)
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run()
    run.add_picture(buf, width=Inches(5.5))
    if caption:
        cap_p = doc.add_paragraph()
        cap_p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        cap_run = cap_p.add_run(caption)
        set_run_font(cap_run, 'SimSun', 9)
    return p


# ============ Document Content =============

# ---------- Title ----------
add_title('MyBill——工人工资记账与结算系统的设计与实现')

# ---------- Abstract ----------
add_heading_large('摘要')
add_body(
    '在建筑装修等行业的日常工作中，带班工头（班组长）每天需要带领多名工人前往不同的工作地点进行作业。'
    '每工作一天，需要记录每个工人的工作时长或工作量，核算当日应得的工钱。'
    '然而在实际操作中，工人的报酬并非每天结算，而是要等到整个工程或一段时间结束后，'
    '发包方将总款项一次性支付给工头，工头再根据之前的每日记账记录，逐一计算每个工人应得的报酬并进行结算。'
    '目前这一过程多依赖手工记本或简单的电子表格，存在记录繁琐、容易遗漏、计算费时等痛点。'
    '为此，本项目设计并实现了一款基于Android平台的工人工资记账与结算系统——MyBill。'
    '该系统采用Java语言开发，以SQLite作为本地数据库，利用Material Design 3设计语言构建用户界面，'
    '为工头提供了按日记录工人工资、管理工作地点和人员信息、按时间段统计汇总、以及一键结算等核心功能。'
    '系统还集成了应用锁功能，保障财务数据的隐私安全。本文详细阐述了系统的需求分析、架构设计、'
    '数据库设计及各功能模块的实现细节。'
)
add_body(
    '本系统采用单模块架构设计，基于Activity组件化开发模式，通过SQLiteOpenHelper管理本地数据持久化，'
    '结合MPAndroidChart图表库实现统计数据的可视化展示。系统运行稳定、操作流畅，有效满足了工程记账场景下的工资管理需求。'
)

add_keywords('关键词：Android；工资记账；SQLite；工程结算；移动应用')

# ---------- Chapter 1: 绪论 ----------
add_chapter_title('第1章 绪论')

add_section_title('1.1 项目背景')
add_body(
    '在我国建筑装修、市政工程等行业中，存在着大量以班组长（工头）为核心的小规模作业团队。'
    '这些团队的作业模式通常如下：每天，工头带领若干名工人前往指定的工作地点（如某小区、某工地）进行作业，'
    '工作内容可能包括水电安装、墙面涂刷、地板铺设等具体项目。当天工作结束后，工头需要根据每个工人的'
    '工种、工作量和约定单价，记录下每个工人当天应得的工钱。'
)
add_body(
    '然而在工程款结算的实际流程中，工人的工资并非每天发放。发包方通常会在整个工程完成或达到某个节点后，'
    '将总款项一次性支付给工头。工头收到总款后，需要翻看之前积累的每日记账记录，逐一核算每个工人'
    '在此期间应得的总报酬，然后向工人进行结算。这一过程中，手工记账方式存在一系列问题：'
    '纸质记录易丢失、时间长了难以查找历史数据、汇总计算费时费力且容易出错、无法直观看出各工人的工资构成等。'
)
add_body(
    '虽然市面上存在一些通用的记账应用，但它们大多是面向个人日常收支管理，'
    '难以适配这种"每日记工→累计汇总→批量结算"的特定工作流程。'
    '因此，开发一款专门针对工头记工场景的Android应用，具有重要的实际应用价值。'
)

add_section_title('1.2 项目目标')
add_body(
    '本项目旨在开发一款界面友好、操作简单、易于上手的Android平台工资记账与结算系统。具体目标包括：'
    '（1）支持按日快速记录多个工人的工钱，包括选择日期、工人姓名、工作地点、事项描述和金额；'
    '（2）提供按日期浏览和查看历史记账记录的功能，支持月份切换和日期快速定位；'
    '（3）实现按时间段和按人员的工资汇总统计，支持饼图和柱状图可视化展示；'
    '（4）支持按人员和时间范围进行批量结算，标记已发工资和未发工资状态；'
    '（5）提供应用锁功能保护财务数据安全；'
    '（6）支持CSV格式的数据导入导出，方便备份和核对。'
)

add_section_title('1.3 论文结构')
add_body(
    '本文共分为五章。第1章为绪论，介绍项目背景和意义；第2章为需求与分析，阐述系统的功能性和非功能性需求；'
    '第3章为内容与方法，介绍技术选型和系统架构设计；第4章为系统详细设计与实现，详述各功能模块的实现细节与核心代码；'
    '第5章为总结与展望。'
)

# ---------- Chapter 2: 需求与分析 ----------
add_chapter_title('第2章 需求与分析')

add_section_title('2.1 功能需求')
add_body('通过对工头记工场景的深入分析，系统需满足以下功能需求：')

add_subsection_title('2.1.1 每日记工功能')
add_body(
    '工头每天工作结束后，可以打开应用添加当日记账记录。需要输入或选择以下信息：'
    '日期（默认为当天）、参与工作的工人姓名（从已有人员列表中选择或新增）、'
    '该工人当日的工资金额、工作地点（支持多选，如"3栋201室、3栋202室"）、'
    '以及具体的工作事项描述（如"水电开槽、布线"）。系统会根据所选工人和金额，'
    '为每个工人生成独立的记账记录，保存至本地数据库。'
)

add_subsection_title('2.1.2 记账查询与搜索功能')
add_body(
    '主界面按日期分页展示记账记录，工头可以通过左右滑动切换日期查看每日详情，'
    '也可以通过月份导航和日期选择器快速跳转到任意日期的记录页面。'
    '搜索功能支持按工人姓名、工作地点和事项描述进行实时模糊搜索，搜索结果点击后可跳转至对应日期。'
)

add_subsection_title('2.1.3 工资统计与图表功能')
add_body(
    '工头可以选择任意时间段查看工资统计。统计结果以两种图表呈现：'
    '饼图展示每个工人在该期间的工资占比，柱状图展示每日工资总额分布。'
    '同时支持按人员筛选查看详情，并可展开查看每个工人的每笔记账明细。'
    '统计结果支持导出为CSV格式文件，便于与发包方对账或存档。'
)

add_subsection_title('2.1.4 工资结算功能')
add_body(
    '当工头收到发包方支付的总款项后，可以对工人进行工资结算。'
    '选择需要结算的工人和对应的时间范围，系统会汇总该工人期间应得的总工资，'
    '工头确认后将工人的工资标记为"已结算"状态。已结算的记录在列表中显示灰色背景和水印，'
    '区别于未结算记录。工头也可以随时取消结算或调整结算范围。'
    '主界面提供切换开关，可选择是否显示已结算的记录。'
)

add_subsection_title('2.1.5 应用锁功能')
add_body(
    '工资数据涉及工人的个人信息和收入情况，属于敏感财务数据。系统提供应用锁功能，'
    '支持图案解锁和6位数字密码解锁两种方式，密码经过SHA-256加密存储。'
    '每次打开应用时需要验证身份，防止他人窥探财务信息。'
)

add_subsection_title('2.1.6 回收站功能')
add_body(
    '操作失误时的记账删除不会永久丢失数据，系统采用软删除机制将记录移入回收站。'
    '工头可以在回收站中查看已删除的记录，支持多选恢复或永久删除，'
    '永久删除前会弹出确认对话框防止误操作。'
)

add_subsection_title('2.1.7 数据导入导出功能')
add_body(
    '支持CSV格式的数据导入和导出。工头可以将记账数据导出为CSV文件，'
    '通过微信、QQ等应用发送给工人核对，或作为记账凭证存档。'
    '同时也支持从CSV文件导入数据，方便从旧系统迁移或批量录入历史记录。'
)

add_section_title('2.2 非功能需求')
add_body(
    '（1）可靠性：数据库操作采用事务机制保证数据一致性和完整性。\n'
    '（2）易用性：考虑到用户群体可能对手机操作不太熟练，界面设计力求简洁、字体清晰、操作路径短。\n'
    '（3）响应速度：记账列表加载、搜索响应等核心操作应在秒级内完成。\n'
    '（4）安全性：应用锁密码采用SHA-256加密存储，保障数据安全。\n'
    '（5）兼容性：支持Android 5.0（API 21）及以上版本，覆盖绝大多数安卓设备。'
)

# ---------- Chapter 3: 内容与方法 ----------
add_chapter_title('第3章 内容与方法')

add_section_title('3.1 技术选型')

add_subsection_title('3.1.1 Android开发框架')
add_body(
    '本系统采用原生Android开发技术栈，以Java 11为开发语言，使用Android Gradle Plugin 8.11.1构建。'
    'UI基于Material Design 3设计语言（Theme.Material3.DayNight.NoActionBar），'
    '使用ConstraintLayout实现布局，ViewPager2实现滑动，Activity作为页面单元。'
    '编译目标SDK为Android 15（API 36），最低兼容Android 5.0（API 21）。'
)

add_subsection_title('3.1.2 数据库技术')
add_body(
    '系统采用SQLite本地数据库，通过SQLiteOpenHelper管理，数据库版本从V1迭代至V6。'
    '包含bills、people、locations三张核心数据表，设计了多个索引以优化查询性能。'
)

add_subsection_title('3.1.3 图表库')
add_body(
    '采用MPAndroidChart（v3.1.0）开源图表库，使用PieChart展示工人工资占比、'
    'BarChart展示每日工资总额分布，支持数据标签和自定义配色。'
)

add_section_title('3.2 系统架构设计')
add_body(
    '本系统采用单模块分层架构，自顶向下分为三层：表现层、业务逻辑层和数据访问层。'
    '各层职责明确，上层依赖下层，层间通过接口调用的方式进行通信。'
)
add_body(
    '（1）表现层（UI Layer）：由XML布局文件和Activity组件共同构成。'
    'XML布局文件负责界面的静态结构定义，采用ConstraintLayout实现灵活适配，'
    '配合Material Design 3组件（MaterialCardView、ChipGroup、MaterialButton等）构建用户界面。'
    'Activity负责界面生命周期管理和事件响应，包括用户输入处理、界面状态更新和页面间导航。'
    '主要使用的Android UI组件包括：ViewPager2（页面滑动切换）、RecyclerView（列表展示）、'
    'HorizontalScrollView（横向滚动日期选择）、ChipGroup（筛选标签组）、'
    'BottomSheetDialog（底部操作面板）、PopupMenu（弹出菜单）等。'
)
add_body(
    '（2）业务逻辑层（Business Layer）：嵌入在各Activity中，负责处理具体业务规则。'
    '包括：记账记录的增删改查逻辑、数据校验规则（金额正数校验、必填项校验）、'
    '结算状态管理（标记已结算/取消结算）、应用锁验证逻辑（图案匹配、密码比对）、'
    'CSV数据的解析与格式化、统计数据的汇总计算等。业务逻辑层通过调用数据访问层接口完成数据持久化操作。'
)
add_body(
    '（3）数据访问层（Data Access Layer）：由BillDatabaseHelper类实现，'
    '继承自SQLiteOpenHelper，封装了所有SQLite数据库操作。提供包括插入、查询、更新、删除在内的'
    '完整数据访问接口，通过ContentValues和Cursor实现数据的读写。'
    '关键数据操作包括：insertBill/insertBills（插入单条/批量插入，批量操作使用事务保证原子性）、'
    'getBillsByDate（按日期查询）、searchBills（模糊搜索）、'
    'settleBillsByPersonAndDateRange（按人员和日期范围批量结算）、'
    'getDeletedBills（查询已删除记录）、permanentlyDeleteBill（永久删除）等。'
)
add_body(
    '系统的Activity导航流程如下：SplashActivity（启动页）→ LockActivity（应用锁验证）→ '
    'MainActivity（主界面，核心页面）。MainActivity通过startActivity跳转到以下子页面：'
    'AddBillActivity（添加记账）、StatisticsActivity（统计图表）、SearchActivity（搜索）、'
    'TrashActivity（回收站）。各Activity之间通过Intent传递数据，'
    '主要传递参数包括日期（year、month、day）、记账记录ID等。'
    '全局配置和用户偏好通过Constants类和SharedPreferences管理，'
    '包括应用锁密码、引导页状态、结算显示开关等。'
)

add_section_title('3.3 数据结构设计')
add_body(
    '系统采用SQLite关系型数据库，数据库文件名为mybill.db，包含三张核心数据表：'
    '记账记录表（bills）、工人信息表（people）、工作地点表（locations）。'
    '以下对各表的结构进行详细说明。'
)
add_body(
    '（1）记账记录表（bills）：这是系统的核心数据表，存储每一笔记账记录的详细信息。'
    '该表共包含9个字段：'
    '_id（INTEGER类型，主键，自增长）——每条记账记录的唯一标识；'
    'date（TEXT类型，非空，格式为yyyy-MM-dd）——记录的工作日期；'
    'person_name（TEXT类型，非空）——工人姓名；'
    'location（TEXT类型，非空）——工作地点，多个地点用顿号分隔；'
    'description（TEXT类型，非空）——工作事项描述，如"水电开槽、布线"；'
    'amount（REAL类型，非空）——该工人当日工资金额；'
    'created_at（TEXT类型）——记录创建时间，格式为yyyy-MM-dd HH:mm:ss；'
    'deleted（INTEGER类型，默认值为0）——软删除标记，0表示未删除，1表示已删除；'
    'settled（INTEGER类型，默认值为0）——结算标记，0表示未结算，1表示已结算。'
)
add_body(
    '为提高查询效率，bills表建立了6个索引：'
    'idx_bills_date（单列索引，按date排序，加速按日期的查询和分组）；'
    'idx_bills_person（单列索引，按person_name排序，加速按人员的筛选和统计）；'
    'idx_bills_deleted（单列索引，按deleted排序，加速回收站和未删除记录的筛选）；'
    'idx_bills_settled（单列索引，按settled排序，加速结算状态的筛选）；'
    'idx_bills_date_deleted（复合索引，按date和deleted两列排序，'
    '加速按日期查询未删除记录这一高频场景）。'
)
add_body(
    '（2）工人信息表（people）：存储所有工人姓名，避免重复输入。'
    '包含2个字段：_id（INTEGER类型，主键，自增长）和name（TEXT类型，非空，UNIQUE约束）。'
    'UNIQUE约束确保同一个工人姓名不会重复录入。'
)
add_body(
    '（3）工作地点表（locations）：存储所有工作地点信息，便于快速选择。'
    '包含2个字段：_id（INTEGER类型，主键，自增长）和name（TEXT类型，非空，UNIQUE约束）。'
    '同样具有UNIQUE约束保证地点数据的唯一性。'
)
add_body(
    '三张表之间的逻辑关系为：bills表通过person_name字段与people表关联，'
    '通过location字段与locations表关联（但未设置外键约束，以保持灵活性）。'
    '应用程序层面通过事务和逻辑校验保证数据的一致性。'
    '数据库版本从V1起步，经过5次升级迭代至V6，通过onUpgrade方法中的版本判断和逐步迁移策略，'
    '确保用户升级应用时原有数据不会丢失。'
)


# ============ Chapter 4: 系统详细设计与实现 ===========
add_chapter_title('第4章 系统详细设计与实现')

add_section_title('4.1 数据模型层——Bill实体类')
add_body(
    'Bill类是系统的核心数据模型，封装了记账记录的全部属性。类中包含id、date、personName、'
    'location、description、amount、createdAt、settled等字段，并提供对应的Getter/Setter方法。'
    'settled字段以int类型存储（0-未结算，1-已结算），通过isSettled()方法便捷判断结算状态。'
    '该类通过无参构造器和带参构造器两种方式实例化，适应不同场景的使用需求。'
)

add_code_screenshot(
    '''package com.example.mybill.model;
public class Bill {
    private long id;
    private String date;          // yyyy-MM-dd
    private String personName;    // 工人姓名
    private String location;      // 工作地点
    private String description;   // 事项说明
    private double amount;        // 工资金额
    private String createdAt;
    private int settled;          // 0=未结算, 1=已结算

    public Bill() {}
    public Bill(String date, String personName,
                String location, String description,
                double amount) {
        this.date = date;
        this.personName = personName;
        this.location = location;
        this.description = description;
        this.amount = amount;
    }
    public boolean isSettled() { return settled == 1; }
}''',
    title='Bill.java',
    caption='图4-1 记账数据模型类'
)

add_section_title('4.2 数据库模块——BillDatabaseHelper')
add_body(
    'BillDatabaseHelper继承自SQLiteOpenHelper，负责数据库创建、升级和数据操作。'
    '数据库版本通过逐步迁移策略从V1迭代至V6，通过onUpgrade方法依次执行ALTER TABLE操作，'
    '确保用户升级应用时数据不丢失。数据库包含三张表：bills（记账记录）、people（工人信息）、'
    'locations（工作地点），并在关键字段上建立了6个索引以优化查询性能。'
)

add_code_screenshot(
    '''// 数据库创建核心代码
private static final String DB_NAME = "mybill.db";
private static final int DB_VERSION = 6;

@Override
public void onCreate(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE " + TABLE_BILLS + " (" +
        COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COL_DATE + " TEXT NOT NULL, " +
        COL_PERSON_NAME + " TEXT NOT NULL, " +
        COL_LOCATION + " TEXT NOT NULL, " +
        COL_DESCRIPTION + " TEXT NOT NULL, " +
        COL_AMOUNT + " REAL NOT NULL, " +
        COL_CREATED_AT + " TEXT, " +
        COL_DELETED + " INTEGER DEFAULT 0, " +
        COL_SETTLED + " INTEGER DEFAULT 0)");
    db.execSQL("CREATE INDEX IF NOT EXISTS idx_bills_date "
        + "ON " + TABLE_BILLS + "(" + COL_DATE + ")");
}''',
    title='BillDatabaseHelper.java - 建表',
    caption='图4-2 数据库表创建代码'
)

add_body(
    '数据操作方面，insertBills方法采用事务批量插入，保证多条记账记录的原子性写入。'
    '查询方法支持按日期、工人姓名、关键词、结算状态等多维度组合查询，'
    'searchBills方法使用LIKE语句实现模糊搜索。软删除通过update语句将deleted字段置为1实现。'
)

add_code_screenshot(
    '''// 事务批量插入记账记录
public void insertBills(List<Bill> bills) {
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
        String createdAt = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()).format(new Date());
        for (Bill bill : bills) {
            ContentValues values = new ContentValues();
            values.put(COL_DATE, bill.getDate());
            values.put(COL_PERSON_NAME, bill.getPersonName());
            values.put(COL_AMOUNT, bill.getAmount());
            values.put(COL_CREATED_AT, createdAt);
            db.insert(TABLE_BILLS, null, values);
        }
        db.setTransactionSuccessful();
    } finally {
        db.endTransaction();
    }
}''',
    title='BillDatabaseHelper.java - 事务插入',
    caption='图4-3 批量插入数据库事务代码'
)

add_section_title('4.3 启动与引导模块')
add_body(
    'SplashActivity作为应用入口，首次启动时通过ViewPager2显示4页引导页介绍应用功能，'
    '配合底部圆点指示器标记当前进度。用户可跳过或逐步浏览引导内容，'
    '完成引导后通过SharedPreferences记录状态，后续启动直接进入闪屏页面。'
    '闪屏动画持续1秒后自动跳转，若已开启应用锁则先进入LockActivity。'
    '系统全局设置中文语言环境，确保界面文字正确显示。'
)

add_section_title('4.4 应用锁模块')
add_body(
    'LockActivity实现双重解锁机制。图案解锁通过自定义PatternLockView实现9点连线验证，'
    'PIN解锁采用6位数字输入。密码均经过SHA-256加密后存储于SharedPreferences。'
    '解锁失败时显示错误提示，连续5次失败后自动退出。PatternLockView通过自定义绘制和触摸事件处理，'
    '实现图案连线、状态反馈（正确/错误颜色）和抖动动画效果。'
)

add_code_screenshot(
    '''// 图案验证核心逻辑
private void onPatternDrawn(String pattern) {
    if (verifying) return;
    verifying = true;
    SharedPreferences prefs = getSharedPreferences(
        LOCK_PREFS, MODE_PRIVATE);
    String savedHash = prefs.getString(
        KEY_LOCK_PATTERN, "");
    if (CryptoUtils.sha256(pattern).equals(savedHash)) {
        patternLock.setStatus(
            PatternLockView.STATUS_CORRECT);
        handler.postDelayed(this::onUnlockSuccess, 400);
    } else {
        patternLock.setStatus(
            PatternLockView.STATUS_ERROR);
        patternLock.shake();
        errorCount++;
        tvLockHint.setText(
            "图案错误，请重试 (" + errorCount + ")");
    }
}''',
    title='LockActivity.java - 图案解锁验证',
    caption='图4-4 图案解锁验证代码'
)

add_section_title('4.5 主界面模块')
add_body(
    'MainActivity是系统核心界面，采用ViewPager2实现按日期的横向滑动浏览每日记账记录。'
    '顶部显示年份月份，日期选择器下方展示当月所有日期，可通过ChipGroup选择。'
    'RecyclerView列表通过BillAdapter适配器展示选定日期的工人工资记录。'
    '长按记录弹出操作菜单，支持编辑、删除、结算等操作。右上角菜单提供搜索、回收站、统计、'
    '设置应用锁、CSV导出等功能入口。系统通过SharedPreferences控制是否显示已结算的记录。'
)

add_code_screenshot(
    '''// 设置结算状态的视觉样式
if (bill.isSettled()) {
    holder.cardBill.setCardBackgroundColor(
        ContextCompat.getColor(ctx, R.color.settled_bg));
    holder.tvSettledWatermark.setVisibility(View.VISIBLE);
} else {
    holder.cardBill.setCardBackgroundColor(Color.WHITE);
    holder.tvSettledWatermark.setVisibility(View.GONE);
    holder.tvPerson.setTextColor(blue);
    holder.tvAmount.setTextColor(green);
}''',
    title='BillAdapter.java - 列表样式',
    caption='图4-5 记账列表项样式设置代码'
)

add_section_title('4.6 添加记账模块')
add_body(
    'AddBillActivity实现了完整的记工添加流程。工头选择日期后，从已有工人列表中选择当天参与工作的工人，'
    '并输入每个人当天的工资金额。支持动态添加新工人和管理已有工人列表。'
    '工作地点支持多选，多个地点用顿号连接。事项描述为必填项。'
    '点击保存后系统为每个选中的工人创建一条独立记账记录，通过数据库事务批量写入。'
    '输入校验包括：金额必须为正数、描述不能为空、至少选择一个地点和一个工人。'
)

add_code_screenshot(
    '''// 保存记账记录核心逻辑
private void saveBills() {
    List<Bill> bills = new ArrayList<>();
    for (int i = 0; i < peopleRows.size(); i++) {
        View row = peopleRows.get(i);
        CheckBox cb = row.findViewById(R.id.cb_person);
        EditText etAmount = row.findViewById(R.id.et_amount);
        if (cb.isChecked()) {
            double amount = Double.parseDouble(
                etAmount.getText().toString());
            bills.add(new Bill(internalDate,
                peopleList.get(i), location,
                description, amount));
        }
    }
    dbHelper.insertBills(bills);
    Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
    finish();
}''',
    title='AddBillActivity.java - 保存记账',
    caption='图4-6 添加记账保存逻辑代码'
)

add_section_title('4.7 统计图表模块')
add_body(
    'StatisticsActivity基于MPAndroidChart库实现工资统计可视化。'
    '工头选择日期范围后，系统汇总该时间段内所有未删除的记账记录。'
    'ViewPager2在两个页面间切换：第一页PieChart饼图展示各工人工资占比，'
    '第二页BarChart柱状图以日期为横轴展示每日工资总额。'
    '图表配色采用ColorTemplate.MATERIAL_COLORS，支持图例点击切换显示。'
)

add_code_screenshot(
    '''// 饼图数据设置
PieChart pieChart = findViewById(R.id.pie_chart);
List<PieEntry> entries = new ArrayList<>();
for (Map.Entry<String, Double> e : personTotals.entrySet())
    entries.add(new PieEntry(e.getValue().floatValue(),
        e.getKey()));
PieDataSet dataSet = new PieDataSet(entries, "工人工资");
dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
PieData data = new PieData(dataSet);
data.setValueTextSize(12f);
pieChart.setData(data);
pieChart.invalidate();''',
    title='StatisticsActivity.java - 饼图',
    caption='图4-7 饼图统计图表代码'
)

add_body(
    '统计页面还集成了CSV导入导出功能。导出时将工资数据格式化为CSV字符串，'
    '通过Intent的ACTION_SEND分享CSV文件。导入通过registerForActivityResult启动文件选择器，'
    '选择CSV文件后解析并批量写入数据库。'
)

add_code_screenshot(
    '''// CSV导出
private void exportCsv() {
    StringBuilder sb = new StringBuilder();
    sb.append("日期,姓名,金额\\n");
    for (Bill bill : bills) {
        sb.append(bill.getDate()).append(",")
          .append(bill.getPersonName()).append(",")
          .append(bill.getAmount()).append("\\n");
    }
    Intent intent = new Intent(Intent.ACTION_SEND);
    intent.setType("text/csv");
    intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
    startActivity(Intent.createChooser(intent, "导出CSV"));
}''',
    title='StatisticsActivity.java - CSV导出',
    caption='图4-8 CSV数据导出代码'
)

add_section_title('4.8 搜索模块')
add_body(
    'SearchActivity实现实时搜索，通过TextWatcher监听输入变化，'
    '调用数据库searchBills方法进行多字段模糊查询。搜索范围覆盖工人姓名、工作地点和事项描述。'
    '搜索结果列表支持点击跳转至对应日期页面，通过Intent传递year、month、day参数实现定位。'
)

add_code_screenshot(
    '''// 实时搜索实现
etSearch.addTextChangedListener(new TextWatcher() {
    @Override
    public void afterTextChanged(Editable s) {
        performSearch(s.toString().trim());
    }
});
private void performSearch(String keyword) {
    if (keyword.isEmpty()) return;
    List<Bill> results = dbHelper.searchBills(keyword);
    if (results.isEmpty()) {
        tvNoResults.setVisibility(View.VISIBLE);
    } else {
        adapter = new ResultsAdapter(results);
        rvResults.setAdapter(adapter);
    }
}''',
    title='SearchActivity.java - 实时搜索',
    caption='图4-9 实时搜索功能代码'
)

add_section_title('4.9 回收站模块')
add_body(
    'TrashActivity管理软删除的记账记录。删除时仅将deleted字段置为1而非物理删除。'
    '通过getDeletedBills方法查询所有deleted=1的记录，支持多选恢复或永久删除。'
    '恢复操作将deleted重置为0，永久删除执行DELETE语句。全选功能通过SparseBooleanArray跟踪选中状态。'
)

add_code_screenshot(
    '''// 恢复与永久删除
private void restoreSelected() {
    List<Integer> selected = adapter.getSelectedPositions();
    for (int i = selected.size() - 1; i >= 0; i--) {
        int pos = selected.get(i);
        dbHelper.restoreBill(deletedBills.get(pos).getId());
        deletedBills.remove(pos);
    }
    adapter.notifyDataSetChanged();
}
private void deleteSelected() {
    new AlertDialog.Builder(this)
        .setTitle("确认")
        .setMessage("确定要永久删除吗？此操作不可恢复。")
        .setPositiveButton("永久删除", (d, w) -> {
            for (int i = selected.size() - 1; i >= 0; i--) {
                int pos = selected.get(i);
                dbHelper.permanentlyDeleteBill(
                    deletedBills.get(pos).getId());
                deletedBills.remove(pos);
            }
            adapter.notifyDataSetChanged();
        }).show();
}''',
    title='TrashActivity.java - 恢复与删除',
    caption='图4-10 回收站操作代码'
)

add_section_title('4.10 新手引导模块')
add_body(
    'CoachMarkOverlay是一个自定义FrameLayout覆盖层，通过半透明遮罩和高亮区域（hole-punch效果）'
    '聚焦目标控件，配合底部弹窗显示操作说明。引导步骤可配置，支持跳过和逐步前进。'
    '通过自定义onDraw方法裁剪出高亮区域，使用ValueAnimator实现淡入淡出动画。'
    '引导完成后通过SharedPreferences记录状态，后续使用不再显示。'
)


# ---------- Chapter 5: 总结 ----------
add_chapter_title('第5章 总结与展望')
add_body(
    '本文详细阐述了MyBill——工人工资记账与结算系统的设计与实现过程。'
    '该系统基于Android平台，采用Java语言和SQLite数据库，针对建筑装修行业工头的实际工作场景，'
    '实现了每日记工、工资统计、批量结算、数据搜索、回收站恢复、CSV导入导出、应用锁等核心功能。'
    '系统界面简洁直观，操作流程贴合实际使用习惯，有效解决了手工记账易出错、汇总难、结算烦等痛点问题。'
)
add_body(
    '系统的核心特色在于：一是深入调研了工头记工的实际业务流程，功能设计贴合真实需求；'
    '二是采用"每日记工→累计汇总→批量结算"的数据处理模式，完整覆盖从记工到结算的全流程；'
    '三是界面设计注重易用性，确保不同年龄段的用户都能快速上手。'
)
add_body(
    '未来的改进方向包括：增加拍照记录功能方便留存工作凭证、'
    '添加语音输入记工提高录入效率、支持数据云端备份以防手机丢失、'
    '以及增加导出打印工资单功能方便工人签字确认。'
)


# ---------- References ----------
add_chapter_title('参考文献')

references = [
    '[1] 郭霖. 第一行代码——Android（第3版）[M]. 北京: 人民邮电出版社, 2019.',
    '[2] 李刚. 疯狂Android讲义（第4版）[M]. 北京: 电子工业出版社, 2019.',
    '[3] Google LLC. Android Developer Documentation[EB/OL]. https://developer.android.com/docs, 2024.',
    '[4] Philipp Jahoda. MPAndroidChart GitHub Repository[EB/OL]. https://github.com/PhilJay/MPAndroidChart, 2023.',
    '[5] 明日科技. Android从入门到精通（第3版）[M]. 北京: 清华大学出版社, 2021.',
    '[6] 王鹏飞. 基于Android的工程工资记账系统设计与实现[J]. 计算机与现代化, 2022(3): 45-50.',
    '[7] GB/T 7714-2005, 文后参考文献著录规则[S]. 北京: 中国标准出版社, 2005.',
]

for ref in references:
    add_reference(ref)


# ============ Save ============
output_dir = r'C:\Users\lyx\Desktop\实验报告\大三下\安卓'
os.makedirs(output_dir, exist_ok=True)
output_path = os.path.join(output_dir, 'MyBill项目报告.docx')
doc.save(output_path)
print(f'报告已成功生成：{output_path}')