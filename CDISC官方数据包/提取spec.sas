/* 1. 设置输出Excel文件的路径 */
/* 请根据实际情况修改下面的路径 */
%let outfile = C:\Project_Web\019_defineXML\CDISC官方数据包\spec.xlsx;

/* 2. 定义宏来提取元数据并生成Excel */
%macro export_metadata(libname);

    /* 获取库中所有数据集的名称 */
    proc sql noprint;
        select memname into :dsn_list separated by ' '
        from dictionary.tables
        where libname = upcase("&libname");
    quit;

    /* 如果库是空的，则停止 */
    %if &dsn_list = %then %do;
        %put WARNING: 库 &libname 中没有找到数据集。;
        %return;
    %end;

    /* 循环处理每一个数据集 */
    %let i = 1;
    %let dsn = %scan(&dsn_list, &i);

    %do %while (&dsn ne );

        /* 
           创建一个临时数据集，用于存放当前数据集的元数据 
           这里完全按照你要求的列名顺序定义变量
        */
        data meta_temp;
            length Domain $8.
                   Seq 8.
                   Variable_Name $32.
                   Variable_Label $200.
                   Type $10.
                   Length 8.
                   Controlled_Terms_or_Format $50.
                   CDISC_Submission_Value $50.
                   Origin $50.
                   Source $50.
                   Role $20.
                   Core $10.
                   SUPP $10.
                   QEVAL $50.
                   Text $200.
                   CRF_Page $50.
                   Comment $200.
                   Method $50.;

            /* 初始化变量为空 */
            Domain = "&dsn";
            Seq = 0; /* 稍后根据变量顺序赋值 */
            Variable_Name = '';
            Variable_Label = '';
            Type = '';
            Length = 0;
            /* 以下字段在SAS字典表中不存在，初始化为空 */
            Controlled_Terms_or_Format = ''; 
            CDISC_Submission_Value = '';
            Origin = ''; 
            Source = ''; 
            Role = ''; 
            Core = ''; 
            SUPP = ''; 
            QEVAL = ''; 
            Text = ''; 
            CRF_Page = ''; 
            Comment = ''; 
            Method = '';

            /* 从字典表中读取实际存在的元数据 */
            set dictionary.columns;
            where libname = upcase("&libname") and memname = upcase("&dsn");

            /* 赋值映射 */
            Seq = varnum;               /* 变量顺序 */
            Variable_Name = name;       /* 变量名 */
            Variable_Label = label;     /* 变量标签 */
            Type = type;                /* 类型 (char/num) */
            Length = length;            /* 长度 */
            
            /* 
               尝试提取格式 (Format)。
               如果有Format，填入该列；如果是标准CT，通常需要外部映射表，这里仅填Format名 
            */
            if not missing(format) then Controlled_Terms_or_Format = strip(format);
            
            /* 输出观测 */
            keep Domain Seq Variable_Name Variable_Label Type Length 
                 Controlled_Terms_or_Format CDISC_Submission_Value Origin Source 
                 Role Core SUPP QEVAL Text CRF_Page Comment Method;
        run;

        /* 
           使用 PROC EXPORT 将临时数据追加到 Excel 的一个新 Sheet 中 
           sheet 名为数据集名
        */
        proc export data=meta_temp
            outfile="&outfile"
            dbms=xlsx
            replace /* 第一次运行时创建文件，后续追加 */
            ;
            sheet="&dsn";
        run;

        /* 扫描下一个数据集 */
        %let i = %eval(&i + 1);
        %let dsn = %scan(&dsn_list, &i);

    %end;

%mend;

/* 3. 执行宏 */
/* 确保你的 sdtm libname 已经分配好 */
%export_metadata(sdtm);
