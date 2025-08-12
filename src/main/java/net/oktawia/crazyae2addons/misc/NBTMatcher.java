package net.oktawia.crazyae2addons.misc;

import appeng.api.stacks.AEItemKey;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.*;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public final class NBTMatcher {

    public static boolean doesItemMatch(@Nullable AEItemKey item, String expr) {
        if (item == null) return false;
        CompoundTag tag = item.getTag();
        if (tag == null) tag = new CompoundTag();
        return doesTagMatch(tag, expr);
    }

    public static boolean doesTagMatch(@Nullable CompoundTag tag, String expr) {
        if (tag == null) tag = new CompoundTag();
        if (expr == null || expr.isBlank()) return false;
        try {
            Node root = new Parser(expr).parseExpression();
            return root.eval(tag);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private interface Node { boolean eval(CompoundTag tag); }

    private record TagNode(CompoundTag crit) implements Node {
        @Override public boolean eval(CompoundTag tag) { return matches(tag, crit); }
    }

    private record OpNode(Node l, Node r, BiFunction<Boolean,Boolean,Boolean> op) implements Node {
        @Override public boolean eval(CompoundTag tag) { return op.apply(l.eval(tag), r.eval(tag)); }
    }

    private record NotNode(Node n) implements Node {
        @Override public boolean eval(CompoundTag tag) { return !n.eval(tag); }
    }

    private static final class Parser {
        private final String src; private int pos;
        Parser(String s){this.src=s;}

        boolean peekInsensitive(String s) {
            return src.regionMatches(true, pos, s, 0, s.length());
        }
        void consumeInsensitive(String s) {
            if (!peekInsensitive(s)) throw err("Expected \"" + s + "\"");
            pos += s.length();
        }

        Node parseExpression() {
            Node n = parseTerm();
            while (true) {
                skipWs();
                if (peek("||") || peekInsensitive("or")) {
                    if (peek("||")) consume(2); else consumeInsensitive("or");
                    n = new OpNode(n, parseTerm(), (a, b) -> a || b);
                } else if (peekInsensitive("xor") || peek("^^")) {
                    if (peekInsensitive("xor")) consumeInsensitive("xor");
                    else consume(2); // ^^
                    n = new OpNode(n, parseTerm(), (a, b) -> a ^ b);
                } else {
                    break;
                }
            }
            return n;
        }

        Node parseTerm() {
            Node n = parseFactor();
            while (true) {
                skipWs();
                if (peek("&&") || peekInsensitive("and")) {
                    if (peek("&&")) consume(2); else consumeInsensitive("and");
                    n = new OpNode(n, parseFactor(), (a, b) -> a && b);
                } else if (peekInsensitive("nand") || peek("!&")) {
                    if (peekInsensitive("nand")) consumeInsensitive("nand");
                    else consume(2); // !&
                    n = new OpNode(n, parseFactor(), (a, b) -> !(a && b));
                } else {
                    break;
                }
            }
            return n;
        }

        Node parseFactor() {
            skipWs();

            if (peek("!") && !peek("!&")) {
                consume(1);
                return new NotNode(parseFactor());
            }
            if (peekInsensitive("not")) {
                consumeInsensitive("not");
                return new NotNode(parseFactor());
            }

            if (peek("(")) {
                consume(1);
                Node n = parseExpression();
                expect(')');
                return n;
            }
            if (peek("{")) {
                String snip = readBalanced();
                return new TagNode(parseSnippet(snip));
            }
            throw err("Nieoczekiwany token");
        }

        private boolean peek(String s){return src.regionMatches(pos,s,0,s.length());}
        private void consume(int n){pos+=n;}
        private void skipWs(){while(pos<src.length()&&Character.isWhitespace(src.charAt(pos)))pos++;}
        private void expect(char c){if(pos>=src.length()||src.charAt(pos)!=c)throw err("Oczekiwano '"+c+"'");pos++;}
        private String readBalanced(){
            int d=0,start=pos;
            do{
                char ch=src.charAt(pos++);
                if(ch=='{')d++;
                else if(ch=='}')d--;
                else if(ch=='\"')skipQuoted();
            }while(d>0&&pos<src.length());
            return src.substring(start,pos);
        }
        private void skipQuoted(){
            while(pos<src.length()){
                char ch=src.charAt(pos++);
                if(ch=='\\')pos++;
                else if(ch=='\"')break;
            }
        }
        private IllegalStateException err(String m){return new IllegalStateException(m+" (pos="+pos+") w '"+src+"'");}
    }

    private static CompoundTag parseSnippet(String raw){
        String s=sanitise(raw);
        try{
            return TagParser.parseTag(s);
        }catch(CommandSyntaxException e){
            throw new IllegalStateException("Bad SNBT: "+raw,e);
        }
    }

    private static String sanitise(String src){
        StringBuilder out=new StringBuilder(src.length()+8);
        boolean inQ=false;
        for(int i=0;i<src.length();i++){
            char ch=src.charAt(i);
            if(ch=='\"'){ out.append(ch); inQ=!inQ; continue; }
            if(!inQ&&ch=='*'){
                boolean key=lookAhead(src,i+1,':');
                boolean val=lookBehind(src,i-1,':');
                if(key||val){ out.append("\"*\""); continue; }
            }
            out.append(ch);
        }
        return out.toString();
    }
    private static boolean lookAhead(String s,int i,char t){while(i<s.length()&&Character.isWhitespace(s.charAt(i)))i++;return i<s.length()&&s.charAt(i)==t;}
    private static boolean lookBehind(String s,int i,char t){while(i>=0&&Character.isWhitespace(s.charAt(i)))i--;return i>=0&&s.charAt(i)==t;}

    private static boolean matches(CompoundTag item, CompoundTag crit){
        for(String ck:crit.getAllKeys()){
            Tag cv=crit.get(ck);

            if("*".equals(ck)){
                if(isAny(cv)){
                    return !item.isEmpty();
                }
                boolean ok=false;
                for(String ik:item.getAllKeys()){
                    if(valueMatches(item.get(ik),cv)){ ok=true; break; }
                }
                if(!ok) return false;
                continue;
            }

            if(!item.contains(ck)) return false;
            if(!valueMatches(item.get(ck),cv)) return false;
        }
        return true;
    }

    private static boolean valueMatches(Tag itemVal, Tag critVal) {
        if (isAny(critVal)) {
            if (itemVal instanceof ListTag itL) {
                return !itL.isEmpty();
            }
            return true;
        }

        if (itemVal instanceof CompoundTag itC && critVal instanceof CompoundTag crC) {
            return matches(itC, crC);
        }

        if (itemVal instanceof ListTag itL && critVal instanceof ListTag crL) {
            return listContains(itL, crL);
        }

        if (itemVal instanceof ListTag itL2 && critVal instanceof CompoundTag crC2) {
            return listAnyMatch(itL2, crC2);
        }

        if (critVal instanceof StringTag strTag) {
            String s = strTag.getAsString();
            if (s.startsWith("!")) {
                String negated = s.substring(1);
                return !itemVal.getAsString().equals(negated);
            }
        }

        return itemVal.equals(critVal);
    }

    private static boolean listContains(ListTag item, ListTag crit){
        if (crit.isEmpty()) {
            return item.isEmpty();
        }
        outer: for(Tag c:crit){
            for(Tag i:item){
                if(valueMatches(i,c)) continue outer;
            }
            return false;
        }
        return true;
    }

    private static boolean listAnyMatch(ListTag item, CompoundTag crit){
        for(Tag i:item) if(i instanceof CompoundTag ct && matches(ct,crit)) return true;
        return false;
    }

    private static boolean isAny(Tag t){ return t instanceof StringTag st && "*".equals(st.getAsString()); }

    private NBTMatcher(){}
}
