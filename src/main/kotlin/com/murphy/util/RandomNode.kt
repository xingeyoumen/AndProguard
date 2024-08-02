package com.murphy.util

import java.util.*
import kotlin.random.Random

/**
 * @property element String or Node or List
 */
class RandomNode(
    private val range: Pair<Int, Int>,
    private val element: Any
) {
    // Random.nextInt(range.first, range.second + 1) 是 Kotlin 中的随机数生成器类。
    // nextInt 是 Random 类中的一个方法，用于生成一个随机整数。
    // 参数 range.first 是生成的随机整数的下界（inclusive），也就是说生成的随机数可以等于 range.first。
    // 参数 range.second + 1 是生成的随机整数的上界（exclusive），也就是说生成的随机数不会等于 range.second + 1。
    private val length: Int
        get() = if (range.first == range.second) range.first
        else if (range.first < range.second) Random.nextInt(range.first, range.second + 1)
        else throw IllegalArgumentException("Wrong length: (${range.first}, ${range.second})")

    private val String.randomChar: Char get() = this[Random.nextInt(0, length)]

    val randomString: String
        get() = when (element) {
            is String -> element
            is StringBuilder -> {
                val content = element.toString()
                String(CharArray(length) { content.randomChar })
            }

            is RandomNode -> {
                val builder = StringBuilder()
                repeat(length) { builder.append(element.randomString) }
                builder.toString()
            }

            is List<*> -> {
                val builder = StringBuilder()
                repeat(length) {
                    element.forEach {
                        builder.append((it as RandomNode).randomString)
                    }
                }
                builder.toString()
            }

            else -> throw IllegalArgumentException("Wrong type: ${element::class.simpleName}")
        }

    companion object {

        fun String.parseNode(): RandomNode {
            val element = parse(this)
            return if (element is RandomNode) element
            else RandomNode(Pair(1, 1), element)
        }

        // "{[1000](1)[0100](3,9)}(2,3)"
        private fun parse(content: String): Any {
            // Stack<String>：这里声明了一个 Stack 类型的对象，该 Stack 是 Java 集合框架中的一种实现，表示一个后进先出（LIFO）的堆栈结构。
            // <String> 表示这个堆栈中存储的是字符串类型的元素。
            // 在 Java 中，Stack 类是一个遗留类，继承自 Vector 类，用于表示堆栈数据结构，其中元素的添加和删除遵循后进先出（LIFO）的原则。
            // Stack 类提供了 push() 方法用于将元素推入堆栈顶部，以及 pop() 方法用于从堆栈顶部弹出并移除元素
            val stack = Stack<String>()
            var pos = content.length - 1
            while (pos > 0) {
                val start = when (content[pos]) {
                    ')' -> content.indexOfLast(pos, '(')
                    ']' -> content.indexOfLast(pos, '[')
                    '}' -> content.indexOf('{')
                    '>' -> {
                        stack.push("(1)")
                        content.indexOfLast(pos, '<')
                    }

                    else -> throw IllegalArgumentException("Wrong parse: $content pos{$pos} char{${content[pos]}}")
                }
                assert(start >= 0) { "Wrong assert: $content pos{$pos} char{${content[pos]}}" }
                stack.push(content.substring(start, pos + 1))
                pos = start - 1
            }
            return if (stack.size == 1) stack.pop().toElement()
            else if (stack.size == 2) {
                val first = stack.pop()
                val second = stack.pop()
                RandomNode(second.toRange(), first.toElement())
            } else if (stack.size % 2 == 0) {
                val list: MutableList<Pair<String, String>> = ArrayList()
                while (!stack.empty()) {
                    list.add(Pair(stack.pop(), stack.pop()))
                }
                list.map { RandomNode(it.second.toRange(), it.first.toElement()) }
            } else throw IllegalArgumentException("Wrong parse: $stack")
        }

        private fun String.toRange(): Pair<Int, Int> {
            if (!startsWith('(') || !endsWith(')'))
                throw IllegalArgumentException("Wrong convert range: $this")
            val split = substring(1, length - 1).split(',')
            val start = split[0].toInt()
            val end = if (split.size == 1) start else split[1].toInt()
            return Pair(start, end)
        }

        //[0100]
        private fun String.toElement(): Any {
            val specCode = substring(1, length - 1)
            if (startsWith('<') && endsWith('>')) return specCode
            if (startsWith('{') && endsWith('}')) return parse(specCode)
            if (startsWith('[') && endsWith(']')) {
                val specPos = specCode.indexOf("1")
                if (specCode.indexOfFirst { it != '1' && it != '0' } > 0 || specCode.length < 4 || specPos < 0 || specPos > 3)
                    throw IllegalArgumentException("Wrong convert element[]: $this")
                val builder = StringBuilder()
                //根据自定义的位置数值信息，添加进入对应的类型，大写，小写，数字，下划线
                if (specCode[0] == '1') builder.append(CHAR_UPPER)
                if (specCode[1] == '1') builder.append(CHAR_LOWER)
                if (specCode[2] == '1') builder.append(CHAR_DIGIT)
                if (specCode[3] == '1') builder.append(CHAR_UNDERLINE)
                return builder
            }
            throw IllegalArgumentException("Wrong convert element: $this")
        }

        private fun CharSequence.indexOfLast(pos: Int, char: Char): Int {
            for (index in indices.reversed()) {
                if (this[index] == char && index < pos) {
                    return index
                }
            }
            return -1
        }
    }
}