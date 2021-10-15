package ru.jawaprogrammer.autolaba02.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Класс обработчик регулярных выражений
 */
public class RegExp {

    private DFAState start;
    private HashSet<DFAState> states;
    private HashSet<DFAState> finishStates;
    //private HashMap<DFAState, HashSet<String>> buffersMap;
    private HashMap<String, StringBuilder> buffers;

    private RegExp() {

    }

    private void init(DFAState start, HashSet<DFAState> finishStates, HashSet<DFAState> states) {
        this.start = start;
        this.finishStates = finishStates;
        this.states = states;
        buffers = new HashMap<>();
    }

    private HashSet<DFAState> byGroup(HashMap<DFAState, Integer> split, int group) {
        HashSet<DFAState> ret = new HashSet<>();
        for (Map.Entry<DFAState, Integer> e : split.entrySet()) {
            if (e.getValue() == group) ret.add(e.getKey());
        }
        return ret;
    }

    public void saveGraphviz(String filename) throws IOException {
        FileWriter fw = new FileWriter(filename);
        fw.write("digraph RegEx {\n" +
                "\n" +
                "    node\n" +
                "        [shape=Mrecord width=1.5];\n" +
                "\n" +
                "    subgraph cluster_RegularExpression {\n");

        DFAState[] stats = new DFAState[states.size()];
        stats = states.toArray(stats);
        HashMap<DFAState, Integer> nameMap = new HashMap<>();
        for (int i = 0; i < stats.length; ++i) {
            nameMap.put(stats[i], i);
            String label = "" + i;
            if (finishStates.contains(stats[i]))
                label += " (accept)";
            if (stats[i] == start)
                label = "(start) " + label;
            fw.write(" \"Automat2Map::" + i + "\"\n" +
                    "[label=\"{" + label + "}\"];\n");
        }
        for (int i = 0; i < stats.length; ++i) {
            for (Map.Entry<Character, DFATransition> tran : stats[i].trans.entrySet()) {
                fw.write(" \"Automat2Map::" + i + "\" -> \"Automat2Map::" + nameMap.get(tran.getValue().next) + "\"\n" +
                        "[label=\"" + tran.getKey() + "\"];\n");
            }
        }

        nameMap.clear();
        nameMap = null;
        fw.write("}}");
        fw.close();
    }


    // минимизация ДКА
    public void minimize() {
        minimize1();
        minimize1();
    }

    // минимизация ДКА
    private void minimize1() {
        // каждому состоянию соответствует имя группы, в которую это состояние входит.
        int groupMax = 2, prew = 0;
        HashMap<DFAState, Integer> split = new HashMap<>();
        for (DFAState st : states) {
            split.put(st, 0);
        }
        for (DFAState st : finishStates) {
            split.put(st, 1);
        }
        boolean changed = true;
        while (changed) { // пока новое разбиение отличается от прошлого
            changed = false;
            prew = groupMax;
            for (int i = 0; i < prew; ++i) { // для каждой группы в разбиении
                HashSet<DFAState> toCheck = byGroup(split, i);
                HashSet<DFAState> proceed = new HashSet<>();
                for (DFAState a : toCheck) {
                    for (DFAState b : toCheck) {
                        if (a == b || proceed.contains(b)) continue;
                        // для каждой пары состояний в группе
                        // у них должны совпадать переходы и для каждого перехода новые состояния должны быть в одной группе
                        // для начала можно создать список всех переходов первого и второго состояния
                        HashSet<Character> trans = new HashSet<>();
                        trans.addAll(a.trans.keySet());
                        trans.addAll(b.trans.keySet());
                        boolean equals = true;
                        for (char ch : trans) {
                            DFATransition ta = a.trans.getOrDefault(ch, null), tb = b.trans.getOrDefault(ch, null);
                            if (ta == null || tb == null || split.get(ta.next) != split.get(tb.next)) {
                                equals = false;
                                break;
                            }
                        }
                        if (!equals) {
                            // переходы переводят в разные группы, или одно из состояний вообще не имеет данного перехода
                            // тогда состояние b будет помещено в новую группу
                            split.put(b, ++groupMax);
                            changed = true;
                        } else {
                            // это на случай, если состояния попали в разные группы по отношению к третьей, хотя друг для друга должны были быть в одной
                            int gr_a = split.get(a), gr_b = split.get(b);
                            if (gr_a != gr_b) changed = true;
                            split.put(b, gr_a);
                        }
                    }
                    proceed.add(a);
                }
                toCheck.clear();
                proceed.clear();
                groupMax = 0;
                for (int mx : split.values()) if (mx > groupMax) groupMax = mx;
            }
        }

        // теперь запоминаем стартовую и принимающие группы и переходы между группами
        int start = -1;
        HashSet<Integer> finish = new HashSet<>();
        HashMap<Integer, HashMap<Character, Integer>> transMap = new HashMap<>();
        for (DFAState st : states) {
            int g = split.get(st);
            if (st == this.start) start = g;
            if (finishStates.contains(st)) finish.add(g);
            HashMap<Character, Integer> trans = transMap.getOrDefault(g, new HashMap<>());
            for (Map.Entry<Character, DFATransition> tr : st.trans.entrySet())
                trans.put(tr.getKey(), split.get(tr.getValue().next));
            transMap.put(g, trans);
        }
        // новые представители групп
        HashMap<Integer, DFAState> newStates = new HashMap<>();
        // Теперь оригинальные состояния нам вообще не нужны. Можем полностью очистить автомат перед его перестройкой.
        clear();
        for (Map.Entry<Integer, HashMap<Character, Integer>> trans : transMap.entrySet()) {
            DFAState stateA = newStates.getOrDefault(trans.getKey(), new DFAState());
            for (Map.Entry<Character, Integer> tr : trans.getValue().entrySet()) {
                DFAState stateB = newStates.getOrDefault(tr.getValue(), new DFAState());
                DFATransition transition = new DFATransition();
                transition.next = stateB;
                stateA.trans.put(tr.getKey(), transition);
                newStates.put(tr.getValue(), stateB);
            }
            newStates.put(trans.getKey(), stateA);
        }
        HashSet<DFAState> finishStates = new HashSet<>();
        for (int i : finish)
            finishStates.add(newStates.get(i));
        init(newStates.get(start), finishStates, new HashSet<>(newStates.values()));
        newStates.clear();
        finish.clear();
        for (Map.Entry<Integer, HashMap<Character, Integer>> trans : transMap.entrySet()) {
            trans.getValue().clear();
        }
        transMap.clear();
    }


    // класс перехода автомата. Позволяет хранить не просто следующее состояние, но и аттрибуты самого перехода
    private class DFATransition {
        DFAState next;
        HashSet<String> activeBuffers, resetBuffers;

        private DFATransition() {
            activeBuffers = new HashSet<>();
            resetBuffers = new HashSet<>();
        }


        public void clear() {
            next = null;
            activeBuffers.clear();
            activeBuffers = null;
        }
    }

    /// класс состояния автомата
    private class DFAState {
        // переходы состояния
        private HashMap<Character, DFATransition> trans;
        private HashSet<Integer> name;

        private DFAState() {
            trans = new HashMap<>();
        }


        public DFAState next(char c) {
            DFATransition nx = trans.getOrDefault(c, null);
            if (nx == null) return null;
            for (String s : nx.resetBuffers)
                RegExp.this.buffers.put(s, new StringBuilder());
            for (String s : nx.activeBuffers) {
                StringBuilder tmp = RegExp.this.buffers.getOrDefault(s, new StringBuilder());
                tmp.append(c);
                RegExp.this.buffers.put(s, tmp);
            }
            return nx.next;
        }

        public void clear() {
            if (trans != null) {
                for (DFATransition tr : trans.values())
                    tr.clear();
                trans.clear();
                trans = null;
            }
            if (name != null) {
                name.clear();
                name = null;
            }
        }
    }

    private DFAState getDFAState() {
        return new DFAState();
    }

    private DFATransition getDFATransition() {
        DFATransition ret = new DFATransition();
        return ret;
    }

    private static class STNode {
        enum NodeType {
            CASE,
            CONCAT,
            CLINI,
            OPTION,
            NAME,
            STRING;

            // в некоторых ситуациях нам будет без разницы, замыкание клини это, или же опция
            public boolean ISOO() {
                return this == CLINI || this == OPTION;
            }
        }

        ///---   тут атрибуты для алгоритма   ---///
        public boolean isNullabel; // может ли данная ветка обнулиться
        public HashSet<Integer> first = new HashSet<>(), last = new HashSet<>(); // множества первых и последних токенов
        ///---   тут атрибуты для алгоритма   ---///

        public NodeType type;
        public String groupName;
        public Token token;
        public STNode parent;
        public STNode childA, childB; // будем работать только до двух потомков, что бы упростить алгоритм

        public STNode(STNode parent, NodeType type, Token t) {
            this.parent = parent;
            token = t;
            this.type = type;
        }

        public STNode(STNode parent, NodeType type, Token t, String name) {
            this.parent = parent;
            token = t;
            groupName = name;
            this.type = type;
        }


        /**
         * полностью очищает данное поддерево
         */
        public void clean() {
            if (childA != null) childA.clean();
            if (childB != null) childB.clean();
            childA = null;
            childB = null;
            type = null;
            parent = null;
            groupName = null;
            token = null;
            first.clear();
            first = null;
            last.clear();
            last = null;
        }

        public void research(HashMap<Integer, HashSet<Integer>> FPs) {
            switch (type) {
                case CONCAT: // конкатенация
                    childA.research(FPs);
                    childB.research(FPs);
                    // каждому из last первого потомка ставим first второго
                    for (int a : childA.last) {
                        if (!FPs.containsKey(a)) FPs.put(a, new HashSet<>());
                        FPs.get(a).addAll(childB.first);
                    }
                    break;
                case CLINI: // клини
                    childA.research(FPs);
                    for (int a : childA.last) {
                        if (!FPs.containsKey(a)) FPs.put(a, new HashSet<>());
                        FPs.get(a).addAll(childA.first);
                    }
                    break;
                case OPTION: // ?
                    childA.research(FPs);

                    break;
                case CASE: // или
                    childA.research(FPs);
                    childB.research(FPs);
                    break;
                case NAME: // ссылка на буфер захвата
                case STRING: // строка
            }
        }

        @Override
        public String toString() {
            return "{\"type\": \"" + type + '"' + (token != null ? ", \"token\": " + token : "") + (groupName != null ? ", \"name\": \"" + groupName + '"' : "") + (childA != null ? ",\n\"A\": " + childA : "") + (childB != null ? ",\n\"B\": " + childB : "") + "}";
        }
    }

    // выделяет из среза токенов узел "или"
    private static STNode findCase(Token[] tokens, int a, int b, STNode parent) {
        // возможны две ситуации:
        // 1) case '|' case
        // 2) concat
        // если мы найдем символ '|' то мы в первой ситуации
        int i = a, ops = 0;
        for (; i < b; ++i) {
            if (tokens[i].type == Token.TokenType.OPEN_SCOPE) ++ops;
            else if (tokens[i].type == Token.TokenType.CLOSE_SCOPE) --ops;
            if (ops < 0)
                throw new RuntimeException("Неверный синтаксис регулярного выражения. Непарная скобка. Примерная позиция: " + i);
            if (ops == 0 && tokens[i].type == Token.TokenType.CASE_LINE)
                break;
        }
        if (ops != 0)
            throw new RuntimeException("Неверный синтаксис регулярного выражения. Непарная скобка. Примерная позиция: " + i);
        if (i == b) // ситуация concat
        {
            STNode toRet = findConcat(tokens, a, b, parent);// избегаем ненужного промежуточного узла
            if (toRet.groupName != null) {
                for (int INT : toRet.first) {
                    HashSet<String> tmp = startBuffersPosAssociation.getOrDefault(INT, new HashSet<>());
                    tmp.add(toRet.groupName);
                    startBuffersPosAssociation.put(i, tmp);
                }
            }
            return toRet;
        } else if (i == b - 1) {
            throw new RuntimeException("Неверный синтаксис регулярного выражения. Операция ИЛИ ('|') должна иметь два аргумента. Примерная позиция: " + i);
        } else { // ситуация case | case
            STNode ret = new STNode(parent, STNode.NodeType.CASE, tokens[i]);
            ret.childA = findCase(tokens, a, i, ret);
            ret.childB = findCase(tokens, i + 1, b, ret);
            ret.isNullabel = ret.childA.isNullabel || ret.childB.isNullabel;
            ret.first.addAll(ret.childA.first);
            ret.first.addAll(ret.childB.first);
            ret.last.addAll(ret.childA.last);
            ret.last.addAll(ret.childB.last);
            return ret;
        }
    }

    private static STNode findConcat(Token[] tokens, int a, int b, STNode parent) {
        // возможны две ситуации - одиночная унарная операция или унарная операция и конкатенация
        // их можно разделить на 4 линейных опций, которые нужно тупо проверить одну за другой.
        // string cl_op
        // string
        // group cl_op
        // group
        STNode first;
        int c = -1;
        if (tokens[a].type.isSN()) // тут проверяются первые четыре опции
        {
            if (a + 1 == b) { // Передали вообще только один токен строки. Он и будет листом сразу
                STNode ret = new STNode(parent, tokens[a].type == Token.TokenType.STRING ? STNode.NodeType.STRING : STNode.NodeType.NAME, tokens[a]);
                ret.first.add(a);
                ret.last.add(a);
                ret.isNullabel = false;
                return ret;
            }
            Token next = tokens[a + 1];
            if (next.type.isCO()) // string cl_op
            {
                first = new STNode(null, next.type == Token.TokenType.QUESTION_SIGN ? STNode.NodeType.OPTION : STNode.NodeType.CLINI, next);
                first.childA = new STNode(first, tokens[a].type == Token.TokenType.STRING ? STNode.NodeType.STRING : STNode.NodeType.NAME, tokens[a]);
                first.isNullabel = true;
                first.childA.first.add(a);
                first.childA.last.add(a);
                first.first.add(a);
                first.last.add(a);
                c = a + 2;
            } else { // string
                first = new STNode(null, tokens[a].type == Token.TokenType.STRING ? STNode.NodeType.STRING : STNode.NodeType.NAME, tokens[a]);
                first.isNullabel = false;
                first.first.add(a);
                first.last.add(a);
                c = a + 1;
            }
        } else if (tokens[a].type == Token.TokenType.OPEN_SCOPE) {
            // Ищем ближайшую парную закрывающую скобку, затем то что внутри скобок оформляем как STNode.
            // И для этого нода в целом процедура такая же как и для строки была раньше (ток меряем не от "a", а от закрывающей скобки)
            int pos = a + 1, ops = 1;
            for (; pos < b && ops != 0; ++pos) {
                if (tokens[pos].type == Token.TokenType.OPEN_SCOPE) ++ops;
                else if (tokens[pos].type == Token.TokenType.CLOSE_SCOPE) --ops;
            }
            if (ops != 0)
                throw new RuntimeException("Неверный синтаксис регулярного выражения. Непарная скобка. Примерная позиция: " + pos);
            int cc = pos - 1; // позиция закрывающихся скобок
            // с группами тоже два случая возможны - они могут начинаться с имени, или без
            STNode group = null;
            if (tokens[a + 1].type == Token.TokenType.CAPTURE_GROUP_NAME) { // у группы есть имя
                group = findCase(tokens, a + 2, cc, null);
                group.groupName = (String) tokens[a + 1].value;
                // так же необходимо занести номера строк и ссылок в специальный список, который позволит в будущем связать состояния с буферами
                for (int i = a + 2; i < cc; ++i)
                    if (tokens[i].type.isSN()) {
                        HashSet<String> tmp = buffersPosAssociation.getOrDefault(i, new HashSet<>());
                        tmp.add(group.groupName);
                        buffersPosAssociation.put(i, tmp);
                    }

            } else group = findCase(tokens, a + 1, cc, null);

            if (group.groupName != null)
                for (int i : group.first) {
                    HashSet<String> tmp = startBuffersPosAssociation.getOrDefault(i, new HashSet<>());
                    tmp.add(group.groupName);
                    startBuffersPosAssociation.put(i, tmp);
                }

            // теперь все как со строками, но начиная с позиции c
            if (cc + 1 == b)// нам передали просто группу
            {
                group.parent = parent;
                return group;
            }
            Token next = tokens[cc + 1];
            if (next.type.isCO()) // group cl_op
            {
                first = new STNode(null, next.type == Token.TokenType.QUESTION_SIGN ? STNode.NodeType.OPTION : STNode.NodeType.CLINI, next);
                first.childA = group;
                group.parent = first;

                first.isNullabel = true;
                first.first.addAll(group.first);
                first.last.addAll(group.last);
                c = cc + 2;
            } else { // group
                first = group;
                c = cc + 1;
            }
        } else {
            throw new RuntimeException("Неверный синтаксис регулярного выражения. Примерное положение: " + a);
        }
        if (first.groupName != null) {
            for (int i : first.first) {
                HashSet<String> tmp = startBuffersPosAssociation.getOrDefault(i, new HashSet<>());
                tmp.add(first.groupName);
                startBuffersPosAssociation.put(i, tmp);
            }
        }
        // возможны 2 случая: нам передали действительно конкатенацию, или же просто одиночный элемент
        // в случае одиночного элемента мы не будем делать лишний узел и сразу вернем first
        if (c == b) {
            first.parent = parent;
            return first;
        } else {
            STNode ret = new STNode(parent, STNode.NodeType.CONCAT, null);
            ret.childA = first;
            ret.childB = findConcat(tokens, c, b, ret);
            ret.isNullabel = ret.childA.isNullabel && ret.childB.isNullabel;
            ret.first.addAll(ret.childA.first);
            if (ret.childA.isNullabel) {
                ret.first.addAll(ret.childB.first);
            }
            ret.last.addAll(ret.childB.last);
            if (ret.childB.isNullabel) {
                ret.last.addAll(ret.childA.last);
            }
            return ret;
        }
    }


    private static class Token {
        enum TokenType {
            REPEAT_AMOUNT,
            ELLIPSIS,
            CAPTURE_GROUP_NAME,
            OPEN_SCOPE,
            CLOSE_SCOPE,
            CASE_LINE,
            QUESTION_SIGN,
            STRING;

            public boolean isCO() {
                return this == QUESTION_SIGN || this == ELLIPSIS;
            }

            public boolean isSN() {
                return this == STRING || this == CAPTURE_GROUP_NAME;
            }
        }

        public Token(TokenType type, Object value) {
            this.type = type;
            this.value = value;
        }

        public Token(TokenType type) {
            this.type = type;
        }

        public Object value;
        public TokenType type;

        @Override
        public String toString() {
            return "{" +
                    "\"type\":\"" + type + '"' +
                    ", \"value\":\"" + value + '"' +
                    '}';
        }
    }

    private final static HashSet<Character> SPECIAL_LETTERS = new HashSet<>(Arrays.asList('{', '}', '<', '>', '(', ')', '|', '?', '%'));
    private static HashMap<Integer, HashSet<String>> buffersPosAssociation, startBuffersPosAssociation;

    /**
     * Метод выполняет построение ДКА по заданному регулярному выражению
     *
     * @param regExp строка регулярного выражения
     * @return объект обертка для ДКА.
     */
    public static RegExp compile(String regExp) {
        /*  Шаг первый - разбить строку на набор лексем согласно правилам
         *  { } ... < > ( ) | ? %
         *  Будет применена следующая оптимизация: если встретил %, то все что идет дальше до следующего % будет считаться просто символами
         *  по этой причине, символы % не будут сохраняться как метасимволы (но нужно учитывать, что %%% это экранирование %
         *  Еще одна оптимизация - имя группы захвата будет выделиться сразу, нет необходимость хранить три токена (<, строка, >)
         *  Аналогично и { число }
         */
        char[] str = regExp.toCharArray();
        LinkedList<Token> list = new LinkedList<>();
        StringBuilder builder = null;
        for (int i = 0; i < str.length; ++i) {
            if (SPECIAL_LETTERS.contains(str[i]) || (str[i] == '.' && i + 2 < regExp.length() && str[i + 1] == '.' && str[i + 2] == '.')) {
                // мы наткнулись на спец символ или троеточие
                if (builder != null) {
                    list.add(new Token(Token.TokenType.STRING, builder.toString())); // то, что накопилось к данному моменту мы сохранили
                    builder = null; // стерли буфер
                }
            } else {
                if (builder == null) builder = new StringBuilder(); // если пусто - начинаем заного
                builder.append(str[i]);
                continue; // мы не натыкались на спец символ. Продолжаем собирать строку
            }
            switch (str[i]) {
                case '{': {
                    StringBuilder buff = new StringBuilder();
                    int start = i;
                    for (++i; i < str.length && str[i] != '}'; ++i)
                        buff.append(str[i]);
                    if (i == str.length)
                        throw new RuntimeException("Неверный синтаксис регулярного выражения. Отсутсвует закрывающая скобка } для скобки на позиции " + i);
                    Integer value = null;
                    try {
                        value = Integer.parseInt(buff.toString());
                        if (value <= 0) throw new NumberFormatException();
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("Неверный синтаксис регулярного выражения. Между { и } допускается использование только положительных чисел");
                    }
                    if (value > 1) // мы избегаем бесполезного {1}
                        list.add(new Token(Token.TokenType.REPEAT_AMOUNT, value));
                }
                continue;
                case '.':
                    if (i + 2 < regExp.length() && str[i + 1] == '.' && str[i + 2] == '.') {
                        i += 2;
                        list.add(new Token(Token.TokenType.ELLIPSIS));
                    }
                    continue;
                case '<': {
                    StringBuilder buff = new StringBuilder();
                    int start = i;
                    for (++i; i < str.length && str[i] != '>'; ++i)
                        buff.append(str[i]);
                    if (i == str.length)
                        throw new RuntimeException("Неверный синтаксис регулярного выражения. Отсутсвует закрывающая скобка > для скобки на позиции " + start);
                    list.add(new Token(Token.TokenType.CAPTURE_GROUP_NAME, buff.toString()));
                }
                continue;
                case '(':
                    list.add(new Token(Token.TokenType.OPEN_SCOPE));
                    continue;
                case ')':
                    list.add(new Token(Token.TokenType.CLOSE_SCOPE));
                    continue;
                case '|':
                    list.add(new Token(Token.TokenType.CASE_LINE));
                    continue;
                case '?':
                    list.add(new Token(Token.TokenType.QUESTION_SIGN));
                    continue;
                case '%':// тут возможны три случая. %% это ошибка. %%% это строка "%", %что-то% значит строка "что-то"
                    if (i + 2 < str.length) {
                        if (str[i + 2] == '%') {// тут обработали случай %a% где a любой символ, даже %
                            list.add(new Token(Token.TokenType.STRING, "" + str[i + 1]));
                            i += 2;
                            continue;
                        }
                        if (str[i + 1] != '%') {
                            StringBuilder buff = new StringBuilder();
                            int start = i;
                            for (++i; i < str.length && str[i] != '%'; ++i)
                                buff.append(str[i]);
                            if (i == str.length)
                                throw new RuntimeException("Неверный синтаксис регулярного выражения. Отсутствует закрывающий экранирующий символ %. Открывающая скобка на позиции " + start);
                            list.add(new Token(Token.TokenType.STRING, buff.toString()));
                            continue;
                        }
                    }
                    throw new RuntimeException("Неверный синтаксис регулярного выражения. Ошибка с символами экранирования %. Примерная позиция " + i);
            }
        }
        // буфер не пуст - последний токен еще не поместили в список
        if (builder != null) {
            list.add(new Token(Token.TokenType.STRING, builder.toString())); // то, что накопилось к данному моменту мы сохранили
            builder = null;
        }
        str = null;
        // Теперь проведем дополнительные оптимизации: конкатенацию строк, удаление бессмысленных операций (по типу ...{n} <=> ...)
        Iterator<Token> it = list.iterator();
        while (it.hasNext()) {
            Token t = it.next();
            // если в списке подряд много строк, то зачем хранить их как отдельные? пусть сразу лежат как одна большая строка
            // это автоматически упрощает будущее построение АСД, так как я точно знаю, что после строки всегда сразу же метасимвол или конец.
            while (t.type == Token.TokenType.STRING && it.hasNext()) {
                Token t2 = it.next();
                if (t2.type == Token.TokenType.STRING) {
                    it.remove();
                    t.value = (String) t.value + t2.value;
                } else {
                    t = t2;
                }
            }
            while (t.type == Token.TokenType.ELLIPSIS && it.hasNext()) { // ...?, ...... и ...{n} действуют так же как и ...
                Token t2 = it.next();
                if (t2.type == Token.TokenType.REPEAT_AMOUNT || t2.type == Token.TokenType.QUESTION_SIGN || t2.type == Token.TokenType.ELLIPSIS) {
                    it.remove();
                } else t = t2;
            }
            while (t.type == Token.TokenType.QUESTION_SIGN && it.hasNext()) { // много знаков вопроса действуют как один
                Token t2 = it.next();
                if (t2.type == Token.TokenType.QUESTION_SIGN) {
                    it.remove();
                } else t = t2;
            }
        }
        // сделать {n} на уровне АСД не получится, так как по алгоритму ссылаться они будут на ту же строку и по факту ничего не изменится
        // по этому {n} будет реализован путем тупого копирования токенов исходной регулярки
        {
            int b = 0;
            Token[] tmp = new Token[list.size()];
            tmp = list.toArray(tmp);
            for (int a = 0; a < tmp.length; ++a, ++b) {
                if (tmp[a].type == Token.TokenType.REPEAT_AMOUNT) {
                    if (a == 0)
                        throw new RuntimeException("Неверный синтаксис регулярного выражения. Ошибка с {n}. Примерная позиция " + a);
                    Token prew = tmp[a - 1];
                    int co = (int) tmp[a].value;
                    if (prew.type == Token.TokenType.STRING) {
                        /// строки тупо наращиваем
                        String base = (String) prew.value;
                        prew.value = base.repeat(Math.max(0, co));
                        list.remove(b);
                        --b;
                    } else if (prew.type == Token.TokenType.CAPTURE_GROUP_NAME) {
                        // имена групп захвата повторяем n раз
                        for (int i = 1; i < co; ++i, ++b)
                            list.add(b, prew);
                        list.remove(b);
                        --b;
                    }
                    // теперь самое сложное - нужно повторять не один элемент, а целый срез: от отрывающих скобок, то закрывающих
                    else if (prew.type == Token.TokenType.CLOSE_SCOPE) {
                        // для начала найдем позицию парной скобки, что бы знать, какой срез нам нужен
                        int ops = 1;
                        int op = -1;
                        for (int i = a - 2; i >= 0; --i) {
                            if (tmp[i].type == Token.TokenType.OPEN_SCOPE) --ops;
                            else if (tmp[i].type == Token.TokenType.CLOSE_SCOPE) ++ops;
                            if (ops == 0) {
                                op = i;
                                break;
                            }
                        }
                        if (op < 0)
                            throw new RuntimeException("Неверный синтаксис регулярного выражения. Непарная скобка. Примерное положение: " + a);
                        List<Token> srez = List.of(Arrays.copyOfRange(tmp, op, a));
                        // теперь просто вставляем срез в список n-1 раз
                        for (int i = 1; i < co; ++i, b += srez.size())
                            list.addAll(b, srez);
                        list.remove(b);
                        --b;
                    } else
                        throw new RuntimeException("Неверный синтаксис регулярного выражения. Использовать {n} можно после группы или строки. Примерное положение: " + a);
                }
            }
        }
        // Аналогом якоря из лекций будет токен STRING с null указателем. Так можно будет понять, что это именно якорь
        list.add(0, new Token(Token.TokenType.OPEN_SCOPE, null));
        list.add(new Token(Token.TokenType.CLOSE_SCOPE, null));
        list.add(new Token(Token.TokenType.STRING, null));
        Token[] tokens = new Token[list.size()];
        tokens = list.toArray(tokens); // Количество токенов меняться больше не будет. Удобнее далее работать с массивом
        list.clear();
        list = null;
/*
        for (Token t : tokens)
            System.out.println(t);
*/

        // Теперь необходимо построить синтаксическое дерево. Листья - строки, а промежуточные узлы - операторы
        // Для построения будем использовать нисходящий рекурсивный разбор. Потому что его я уже знаю, а многократное сканирование с лекции пересматривать лень)))
        // В данном алгоритме менее приоритетная операция является предком для более приоритетной. Строка делится на две части и обе обрабатываются рекурсивно.
        // Так как я уже соединил все подряд идущие строки меж собой, данное АСД почти ничем не отличается от АСД для математических выражений.
        buffersPosAssociation = new HashMap<>();
        startBuffersPosAssociation = new HashMap<>();
        STNode root = findCase(tokens, 0, tokens.length, null);
        System.out.println(root);

        // Следующий этап построения: обход дерева с целью восстановления FP множеств токенов
        HashMap<Integer, HashSet<Integer>> FPs = new HashMap<>();
        root.research(FPs);
        // Так как до этого я не хранил отдельно букв, только цельные строки-токены, дальше придется выкручиваться))
        // Будут два основных типа состояний - именованные и безымянные.
        // Именованные состояния работают так же как на лекции, у них есть "представитель" (множество FP)
        // Безымянные состояния будут отвечать за промежуточные буквенные переходы. Их будет несложно создать, так как строка это конкатенация букв.
        // Имена будут так же реализованы с помощью HashMap. Причем с новым уровнем извращения - ключ это HashSet))

        // это нумер якоря. Потом будем с помощью этого числа назначать множества принимающих состояний.
        final int finit = tokens.length - 1;

        // множество необработанных состояний
        Queue<DFAState> unproceed = new LinkedList<>();
        // множество именованных состояний
        HashMap<HashSet<Integer>, DFAState> names = new HashMap<>();

        //HashMap<DFAState, HashSet<String>> buffersMap = new HashMap<>();

        RegExp toReturn = new RegExp();

        HashSet<DFAState> states = new HashSet<>();
        DFAState start = toReturn.getDFAState();
        states.add(start);

        start.name = root.first;
        names.put(root.first, start);
        unproceed.add(start);

        while (!unproceed.isEmpty()) {
            /// достаточно для каждого токена из имени состояния проложить "тропинку" к следующему состоянию
            /// если придет нерассмотренный символ - будет возвращено null и автомат остановится
            DFAState next = unproceed.poll();
            // для того, что бы одинаковые строки не затмевали друг друга, придется ввести словарь всех локальных строк
            // и каждой строке добавить множества следующих позиций
            HashMap<String, HashSet<Integer>> FPsStr = new HashMap<>();
            for (int st : next.name) {
                if (st == finit) continue;
                String strng = (String) tokens[st].value;
                HashSet<Integer> set = FPsStr.getOrDefault(strng, null);
                if (set == null) {
                    set = new HashSet<>();
                    FPsStr.put(strng, set);
                }
                set.addAll(FPs.get(st));
            }
            for (int st : next.name) {
                // пока что игнорирую тот факт, что это может оказаться имя буфера, а не строка.
                if (st == finit) continue; // якорь обрабатывать не надо)
                DFAState cur = next;
                String strng = (String) tokens[st].value;
                char[] arr = strng.toCharArray();

                HashSet<String> curBuffs = buffersPosAssociation.getOrDefault(st, null);

              /*  if (curBuffs != null) {
                    HashSet<String> tmp = buffersMap.getOrDefault(cur, new HashSet<>());
                    tmp.addAll(curBuffs);
                    buffersMap.put(cur, tmp);
                }*/

                HashSet<Integer> nexts = FPsStr.get(strng); // получили множества следующих состояний (с учетом того, что строки могут совпадать)
                DFAState nnnx = names.getOrDefault(nexts, null);
                if (nnnx == null) {
                    nnnx = toReturn.getDFAState();
                    states.add(nnnx);
                    nnnx.name = nexts;
                    unproceed.add(nnnx);
                    names.put(nexts, nnnx);
                }

                for (int i = 0; i < arr.length - 1; ++i) {
                    /// генератор "пути". Ищет оптимально (например у (me|mem) будет общее начало, но после е будет расщепление путей
                    DFATransition nx = cur.trans.getOrDefault(arr[i], null);
                    if (nx == null) {
                        nx = toReturn.getDFATransition();
                        nx.next = toReturn.getDFAState();
                        states.add(nx.next);
                        cur.trans.put(arr[i], nx);
                    }
                    if (curBuffs != null) {
                        nx.activeBuffers.addAll(curBuffs);
                    }
                    if (i == 0) {
                        for (int a : nnnx.name) {
                            HashSet<String> tmp = startBuffersPosAssociation.getOrDefault(a, null);
                            if (tmp != null)
                                nx.resetBuffers.addAll(tmp);
                        }
                    }
                    cur = nx.next;
                }
                // теперь в cur лежит последнее безымянное состояние данного пути. Ему нужно задать переход в nexts

                DFATransition nxtr = toReturn.getDFATransition();
                nxtr.next = nnnx;
                if (curBuffs != null) {
                    nxtr.activeBuffers.addAll(curBuffs);
                }
                if (arr.length == 1) {
                    for (int a : nnnx.name) {
                        HashSet<String> tmp = startBuffersPosAssociation.getOrDefault(a, null);
                        if (tmp != null)
                            nxtr.resetBuffers.addAll(tmp);
                    }
                }
                cur.trans.put(arr[arr.length - 1], nxtr);
            }
            FPsStr.clear();
        }
        // остается только назначить конечные состояния и автомат будет готов.
        HashSet<DFAState> finish = new HashSet<>();
        for (HashSet<Integer> ks : names.keySet()) {
            DFAState tmp = names.get(ks);
            if (ks.contains(finit)) finish.add(tmp);
            tmp.name = null;
        }
        names.clear();
        for (HashSet<Integer> a : FPs.values()) a.clear();
        FPs.clear();
        buffersPosAssociation.clear();
        buffersPosAssociation = null;
        startBuffersPosAssociation.clear();
        startBuffersPosAssociation = null;
        root.clean();
        System.gc(); // неплохо бы подчистить мусор))
        toReturn.init(start, finish, states);
        return toReturn;
    }

    /**
     * Проверяет, принадлежит ли строка языку, описываемому данным выражением.
     *
     * @param s строка для проверки
     * @return true, если принадлежит
     */
    public boolean checkString(String s) {
        DFAState cur = start;
       /* {
            HashSet<String> bufs = buffersMap.getOrDefault(cur, null);
            if (bufs != null) {
                for (String str : bufs) {
                    buffers.put(str, new StringBuilder());
                }
            }
        }*/
        char[] arr = s.toCharArray();
        for (char c : arr) {
            //HashSet<String> bufs1 = buffersMap.getOrDefault(cur, null);
            cur = cur.next(c);
            //HashSet<String> bufs2 = buffersMap.getOrDefault(cur, null);
            if (cur == null)
                return false; // неопределенный переход значит, что автомат досрочно может вынести отрицательный вердикт
            /*if (bufs1 == null && bufs2 != null) {
                for (String str : bufs2) {
                    buffers.put(str, new StringBuilder());
                }
            }
            if (bufs1 != null) {
                for (String str : bufs1)
                    buffers.get(str).append(c);

            }*/
        }
        return finishStates.contains(cur);
    }

    /**
     * Очищает объект регулярного выражения, высвобождает ресурсы. После вызова данного метода, экземпляр класс больше не работоспособен.
     */
    public void clear() {
        for (DFAState st : states)
            st.clear();
        states.clear();
        start.clear();
        finishStates.clear();
    }
}
