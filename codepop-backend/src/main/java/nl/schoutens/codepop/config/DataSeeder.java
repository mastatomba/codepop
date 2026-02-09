package nl.schoutens.codepop.config;

import jakarta.annotation.PostConstruct;
import nl.schoutens.codepop.entity.Question;
import nl.schoutens.codepop.entity.QuestionOption;
import nl.schoutens.codepop.entity.Topic;
import nl.schoutens.codepop.repository.QuestionOptionRepository;
import nl.schoutens.codepop.repository.QuestionRepository;
import nl.schoutens.codepop.repository.TopicRepository;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder {

    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;

    public DataSeeder(TopicRepository topicRepository, 
                     QuestionRepository questionRepository,
                     QuestionOptionRepository questionOptionRepository) {
        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
        this.questionOptionRepository = questionOptionRepository;
    }

    @PostConstruct
    public void seed() {
        // Check if data already exists (idempotent)
        if (topicRepository.count() > 0) {
            return;
        }

        seedTopics();
        seedJavaRecordsQuestions();
        seedJavascriptReactQuestions();
    }

    private void seedTopics() {
        // Backend topics
        topicRepository.save(new Topic("Java", "backend"));
        topicRepository.save(new Topic("Python", "backend"));
        topicRepository.save(new Topic("Node.js", "backend"));
        topicRepository.save(new Topic("C#", "backend"));
        topicRepository.save(new Topic("Go", "backend"));
        topicRepository.save(new Topic("Rust", "backend"));
        topicRepository.save(new Topic("PHP", "backend"));

        // Frontend topics
        topicRepository.save(new Topic("JavaScript", "frontend"));
        topicRepository.save(new Topic("TypeScript", "frontend"));
        topicRepository.save(new Topic("React", "frontend"));
        topicRepository.save(new Topic("Vue", "frontend"));
        topicRepository.save(new Topic("Angular", "frontend"));
        topicRepository.save(new Topic("HTML", "frontend"));
        topicRepository.save(new Topic("CSS", "frontend"));
        topicRepository.save(new Topic("Svelte", "frontend"));

        // Mobile topics
        topicRepository.save(new Topic("Swift", "mobile"));
        topicRepository.save(new Topic("Kotlin", "mobile"));
        topicRepository.save(new Topic("React Native", "mobile"));
        topicRepository.save(new Topic("Flutter", "mobile"));
    }

    private void seedJavaRecordsQuestions() {
        Topic topic = topicRepository.findByNameIgnoreCase("Java").orElseThrow();

        // Question 1: Easy with explanation
        Question q1 = questionRepository.save(new Question(
            topic,
            "What keyword is used to define a record in Java?",
            "records",
            Question.Difficulty.EASY,
            "Java records use the 'record' keyword introduced in Java 14 as a preview feature and made permanent in Java 16."
        ));
        questionOptionRepository.save(new QuestionOption(q1, "record", true));
        questionOptionRepository.save(new QuestionOption(q1, "class", false));
        questionOptionRepository.save(new QuestionOption(q1, "struct", false));
        questionOptionRepository.save(new QuestionOption(q1, "data", false));
        questionOptionRepository.save(new QuestionOption(q1, "entity", false));

        // Question 2: Medium without explanation
        Question q2 = questionRepository.save(new Question(
            topic,
            "Are Java records mutable or immutable?",
            "records",
            Question.Difficulty.MEDIUM,
            null
        ));
        questionOptionRepository.save(new QuestionOption(q2, "Immutable", true));
        questionOptionRepository.save(new QuestionOption(q2, "Mutable", false));
        questionOptionRepository.save(new QuestionOption(q2, "Depends on configuration", false));
        questionOptionRepository.save(new QuestionOption(q2, "Both mutable and immutable", false));

        // Question 3: Hard with explanation
        Question q3 = questionRepository.save(new Question(
            topic,
            "Can a Java record implement interfaces?",
            "records",
            Question.Difficulty.HARD,
            "Records can implement interfaces but cannot extend other classes since they implicitly extend java.lang.Record."
        ));
        questionOptionRepository.save(new QuestionOption(q3, "Yes, records can implement interfaces", true));
        questionOptionRepository.save(new QuestionOption(q3, "No, records cannot implement anything", false));
        questionOptionRepository.save(new QuestionOption(q3, "Only marker interfaces", false));
        questionOptionRepository.save(new QuestionOption(q3, "Yes, but only functional interfaces", false));
        questionOptionRepository.save(new QuestionOption(q3, "Records can only extend classes", false));
        questionOptionRepository.save(new QuestionOption(q3, "Records support multiple inheritance", false));
    }

    private void seedJavascriptReactQuestions() {
        Topic topic = topicRepository.findByNameIgnoreCase("React").orElseThrow();

        // Question 1: Easy with explanation
        Question q1 = questionRepository.save(new Question(
            topic,
            "What is the purpose of the useState hook in React?",
            "hooks",
            Question.Difficulty.EASY,
            "useState is a React Hook that lets you add state to functional components."
        ));
        questionOptionRepository.save(new QuestionOption(q1, "To manage state in functional components", true));
        questionOptionRepository.save(new QuestionOption(q1, "To handle side effects", false));
        questionOptionRepository.save(new QuestionOption(q1, "To fetch data from APIs", false));
        questionOptionRepository.save(new QuestionOption(q1, "To create context", false));

        // Question 2: Medium without explanation
        Question q2 = questionRepository.save(new Question(
            topic,
            "Which method is called after a component is rendered for the first time?",
            "lifecycle",
            Question.Difficulty.MEDIUM,
            null
        ));
        questionOptionRepository.save(new QuestionOption(q2, "componentDidMount (class) or useEffect with empty deps (hooks)", true));
        questionOptionRepository.save(new QuestionOption(q2, "componentWillMount", false));
        questionOptionRepository.save(new QuestionOption(q2, "render", false));
        questionOptionRepository.save(new QuestionOption(q2, "constructor", false));
        questionOptionRepository.save(new QuestionOption(q2, "componentDidUpdate", false));

        // Question 3: Hard with explanation
        Question q3 = questionRepository.save(new Question(
            topic,
            "What is the virtual DOM in React?",
            "fundamentals",
            Question.Difficulty.HARD,
            "The virtual DOM is a lightweight copy of the actual DOM. React uses it to optimize updates by comparing changes and only updating what's necessary."
        ));
        questionOptionRepository.save(new QuestionOption(q3, "A lightweight representation of the real DOM", true));
        questionOptionRepository.save(new QuestionOption(q3, "A database for storing component state", false));
        questionOptionRepository.save(new QuestionOption(q3, "A browser API for DOM manipulation", false));
        questionOptionRepository.save(new QuestionOption(q3, "A CSS framework", false));
        questionOptionRepository.save(new QuestionOption(q3, "A server-side rendering technique", false));
        questionOptionRepository.save(new QuestionOption(q3, "A testing library", false));
    }
}
